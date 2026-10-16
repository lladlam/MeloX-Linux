package melox

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import melox.ui.MeloXApp
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Linux entry. User data stays under ~/.melox (see MeloXStorage) so a
 * different working directory, including an AppImage mount, cannot move it.
 */
fun main() {
    pinDataHome()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        writeCrash("MeloX-crash", thread, throwable)
    }

    try {
        application {
            val windowState = rememberWindowState(
                width = 427.dp,
                height = 924.dp,
                position = WindowPosition.PlatformDefault,
            )
            Window(
                title = "MeloX",
                state = windowState,
                onCloseRequest = ::exitApplication,
            ) {
                window.minimumSize = java.awt.Dimension(360, 640)
                MeloXApp()
            }
        }
    } catch (e: Exception) {
        writeCrash("MeloX-startup-error", Thread.currentThread(), e)
        throw e
    }
}

private fun pinDataHome() {
    val home = System.getProperty("user.home")?.takeIf { it.isNotBlank() } ?: return
    File(home, ".melox").mkdirs()
    File(home, ".melox/cache").mkdirs()
    File(home, ".melox/logs").mkdirs()
}

private fun writeCrash(prefix: String, thread: Thread, throwable: Throwable) {
    val sw = StringWriter()
    throwable.printStackTrace(PrintWriter(sw))
    val stackTrace = sw.toString()
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
    val home = System.getProperty("user.home").orEmpty().ifBlank { "." }
    val logDir = File(home, ".melox/logs").apply { mkdirs() }
    val logFile = File(logDir, "$prefix-$timestamp.log")
    val body = """
        |MeloX Linux Crash Log
        |======================
        |Time: ${Date()}
        |Thread: ${thread.name}
        |user.dir: ${System.getProperty("user.dir")}
        |Exception: ${throwable.javaClass.name}: ${throwable.message}
        |
        |Stack Trace:
        |$stackTrace
    """.trimMargin()
    runCatching { logFile.writeText(body) }
    System.err.println(body)
}
