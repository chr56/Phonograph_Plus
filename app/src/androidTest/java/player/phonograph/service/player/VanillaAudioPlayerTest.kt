package player.phonograph.service.player

import android.media.MediaPlayer
import android.test.AndroidTestCase
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Regression test for [VanillaAudioPlayer.stop] when MediaPlayer.reset() throws (#372).
 */
@Suppress("DEPRECATION")
class VanillaAudioPlayerTest : AndroidTestCase() {

    fun testStopRecoversFromResetFailure() {
        val audioFile = File(context.cacheDir, "vanilla-audio-player-test.wav")
        writeSilentWav(audioFile)
        val player = VanillaAudioPlayer(context, false)
        try {
            assertTrue(player.setDataSource(audioFile.absolutePath))

            // A released MediaPlayer throws IllegalStateException from reset().
            val broken = currentMediaPlayer(player)
            broken.release()

            player.stop()

            assertFalse(player.isInitialized)
            assertNotSame(broken, currentMediaPlayer(player))
        } finally {
            player.release()
            audioFile.delete()
        }
    }

    private fun currentMediaPlayer(player: VanillaAudioPlayer): MediaPlayer {
        val field = VanillaAudioPlayer::class.java.getDeclaredField("currentMediaPlayer")
        field.isAccessible = true
        return field.get(player) as MediaPlayer
    }

    /** One second of 8 kHz mono 16-bit silence. */
    private fun writeSilentWav(file: File) {
        val sampleRate = 8_000
        val dataSize = sampleRate * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }
        file.writeBytes(header.array() + ByteArray(dataSize))
    }
}
