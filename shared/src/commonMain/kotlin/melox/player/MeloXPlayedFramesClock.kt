package melox.player

/**
 * Heard-frame accounting for the progress **display**.
 *
 * Java Sound's SourceDataLine.longFramePosition is accurate on WASAPI and native
 * ALSA, but the ALSA -> PulseAudio path used by many Linux desktops reports a
 * counter that jumps to the buffer size and then stalls, which would freeze the
 * player time label at 0:00. Trust the device while it advances; once it stalls
 * while PCM keeps flowing, estimate progress from the wall clock (Java Sound
 * consumes in real time) and clamp it to the frames the line has accepted so
 * the estimate can never run past decoded audio.
 *
 * Engine semantics (handoff, end-of-track drain, rendered-frame accounting)
 * deliberately keep reading the raw device counter.
 */
internal class MeloXPlayedFramesClock(private val isPaused: () -> Boolean) {
    private var played = 0L
    private var deviceLast = Long.MIN_VALUE
    private var wallBaseNs = -1L
    private var wallBaseFrames = 0L

    /** Reset for a new output line / seek restart. */
    @Synchronized
    fun reset() {
        played = 0L
        deviceLast = Long.MIN_VALUE
        wallBaseNs = -1L
        wallBaseFrames = 0L
    }

    /** deviceFrames: the raw counter, or null when the device query failed. */
    @Synchronized
    fun observe(deviceFrames: Long?, writtenFrames: Long): Long {
        val written = writtenFrames.coerceAtLeast(0L)
        if (isPaused()) {
            // Freeze progress and rebase the wall clock on resume.
            wallBaseNs = -1L
            if (deviceFrames != null && deviceFrames > deviceLast) deviceLast = deviceFrames
            return played.coerceIn(0L, written)
        }
        if (deviceFrames != null) {
            val advanced = deviceLast != Long.MIN_VALUE && deviceFrames > deviceLast
            if (advanced || deviceFrames >= written) {
                // The device counter is alive (or playback has fully drained).
                played = deviceFrames.coerceIn(played, written)
                deviceLast = deviceFrames
                wallBaseNs = -1L
                return played
            }
            deviceLast = deviceFrames
        }
        played = played.coerceIn(0L, written)
        if (wallBaseNs < 0L) {
            wallBaseNs = System.nanoTime()
            wallBaseFrames = played
            return played
        }
        val wallFrames = (System.nanoTime() - wallBaseNs) * AudioPlayer.PCM_SAMPLE_RATE / 1_000_000_000L
        played = (wallBaseFrames + wallFrames).coerceIn(played, written)
        return played
    }

    /** Frames accepted by the line but not yet heard. */
    @Synchronized
    fun unplayed(writtenFrames: Long): Long = (writtenFrames.coerceAtLeast(0L) - played).coerceAtLeast(0L)
}
