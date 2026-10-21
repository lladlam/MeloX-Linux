package melox.provider.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import melox.platform.meloXDataDir
import java.io.File
import java.security.MessageDigest

class LocalMediaScanner(
    private val repository: LocalMusicRepository = LocalMusicRepository(),
) {
    private val audioExtensions = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma")

    suspend fun scanAll(): List<LocalTrackRecord> = withContext(Dispatchers.IO) {
        val roots = repository.scanRoots().mapNotNull { root ->
            File(root.uri.removePrefix("file://")).takeIf(File::isDirectory)
        } + listOf(defaultMusicDirectory())
        val existing = repository.tracks().associateBy(LocalTrackRecord::fileKey)
        val records = roots.flatMap { scanDirectory(it) }.distinctBy(LocalTrackRecord::fileKey).map { scanned ->
            existing[scanned.fileKey]?.let { old -> scanned.copy(
                isFavorite = old.isFavorite, artworkUri = scanned.artworkUri ?: old.artworkUri,
                recognizedNeteaseId = old.recognizedNeteaseId, recognizedTitle = old.recognizedTitle,
                recognizedArtist = old.recognizedArtist, recognizedAlbum = old.recognizedAlbum,
                recognizedArtworkUrl = old.recognizedArtworkUrl, cachedLyrics = old.cachedLyrics,
                // Keep a known duration while the file itself did not change.
                durationMs = old.durationMs.takeIf { it > 0L && old.lastModifiedMs == scanned.lastModifiedMs } ?: scanned.durationMs,
            ) } ?: scanned
        }.map { record ->
            if (record.durationMs > 0L) record else record.copy(durationMs = probeDurationMs(record))
        }
        repository.replaceTracks(records)
        records
    }

    private fun defaultMusicDirectory(): File =
        File(meloXDataDir(), "music").takeIf(File::isDirectory) ?: File(System.getProperty("user.home"), "Music")

    /** Read Duration from the bundled FFmpeg so desktop playback can show and seek real time. */
    private fun probeDurationMs(record: LocalTrackRecord): Long {
        val command = melox.player.FfmpegLocator.command()
        if (command.isEmpty()) return 0L
        val file = runCatching {
            when {
                record.contentUri.startsWith("file:", true) -> File(java.net.URI(record.contentUri))
                record.contentUri.contains("://") -> null
                else -> File(record.contentUri)
            }
        }.getOrNull()
        if (file == null || !file.isFile) return 0L
        return runCatching {
            val process = ProcessBuilder(command + listOf("-hide_banner", "-i", file.absolutePath, "-t", "0.01", "-f", "null", "-"))
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return 0L
            }
            Regex("""Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)""").find(output)?.let { match ->
                val (hours, minutes, seconds) = match.destructured
                val totalMs = (hours.toLong() * 3600 + minutes.toLong() * 60) * 1000 + (seconds.toDouble() * 1000).toLong()
                totalMs.takeIf { it > 0L }
            } ?: 0L
        }.getOrDefault(0L)
    }

    private fun scanDirectory(directory: File): List<LocalTrackRecord> = buildList {
        val children = runCatching { directory.listFiles()?.toList() }.getOrNull().orEmpty()
        children.forEach { child ->
            when {
                child.isDirectory -> addAll(scanDirectory(child))
                child.isFile && child.extension.lowercase() in audioExtensions -> {
                    runCatching { scanFile(child) }.getOrNull()?.let(::add)
                }
            }
        }
    }

    private fun scanFile(file: File): LocalTrackRecord {
        val baseName = file.nameWithoutExtension
        val (artist, title) = splitFileName(baseName)
        return LocalTrackRecord(
            fileKey = fileKey(file),
            contentUri = file.toURI().toString(),
            displayName = file.name,
            title = title,
            artist = artist,
            album = file.parentFile?.name.orEmpty(),
            durationMs = 0L,
            mimeType = "audio/${file.extension.lowercase()}",
            sizeBytes = file.length(),
            lastModifiedMs = file.lastModified(),
            sourceRootUri = file.toURI().toString(),
        )
    }

    private fun splitFileName(baseName: String): Pair<String, String> {
        val dash = baseName.indexOf(" - ")
        return if (dash > 0) {
            baseName.substring(0, dash).trim() to baseName.substring(dash + 3).trim()
        } else {
            "" to baseName.trim()
        }
    }

    private fun fileKey(file: File): String = MessageDigest.getInstance("SHA-256")
        .digest("${file.absolutePath}:${file.length()}".toByteArray())
        .joinToString("") { "%02x".format(it) }
        .take(24)
}
