package player.phonograph.service.player

import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.media.MediaBrowserServiceCompat
import player.phonograph.App

/**
 * @author chr_56 & Abou Zeid (kabouzeid) (original author)
 */
@Suppress("DEPRECATION")
class AudioFocusManager {
    private val audioManager: AudioManager by lazy {
        App.instance.getSystemService(MediaBrowserServiceCompat.AUDIO_SERVICE) as AudioManager
    }
    fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_GAIN
    }
    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        } else {
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }
    private val audioFocusRequest: AudioFocusRequest? by lazy(LazyThreadSafetyMode.NONE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setOnAudioFocusChangeListener(
                audioFocusListener
            ).build()
        } else { null }
    }
    private val audioFocusListener: AudioManager.OnAudioFocusChangeListener by lazy(
        LazyThreadSafetyMode.NONE
    ) {
        AudioManager.OnAudioFocusChangeListener { focusChange -> }
        // todo
    }
}
