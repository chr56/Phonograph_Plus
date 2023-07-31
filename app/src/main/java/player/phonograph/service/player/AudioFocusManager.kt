package player.phonograph.service.player

import player.phonograph.service.player.PlayerController.Companion.PAUSE_FOR_LOSS_OF_FOCUS
import player.phonograph.service.player.PlayerController.Companion.PAUSE_FOR_TRANSIENT_LOSS_OF_FOCUS
import player.phonograph.service.player.PlayerController.ControllerHandler.Companion.DUCK
import player.phonograph.service.player.PlayerController.ControllerHandler.Companion.UNDUCK
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import kotlin.LazyThreadSafetyMode.NONE

/**
 * @author chr_56 & Abou Zeid (kabouzeid) (original author)
 */
class AudioFocusManager(private val controller: PlayerController) : AudioManager.OnAudioFocusChangeListener {

    private val audioManager: AudioManager = controller.service.getSystemService(AUDIO_SERVICE) as AudioManager

    fun requestAudioFocus(): Boolean {
        return AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest) == AudioManager.AUDIOFOCUS_GAIN
    }

    fun abandonAudioFocus() {
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
    }

    private val audioFocusRequest: AudioFocusRequestCompat by lazy(NONE) {
        AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(this, controller.handler)
            .setAudioAttributes(
                AudioAttributesCompat.Builder()
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .build()
            )
            .build()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN                    -> {
                if (!controller.isPlaying()) {
                    when (controller.pauseReason) {
                        PAUSE_FOR_TRANSIENT_LOSS_OF_FOCUS -> controller.play()
                        PAUSE_FOR_LOSS_OF_FOCUS           -> {}
                    }
                }
                controller.handler.removeMessages(DUCK)
                controller.handler.sendEmptyMessage(UNDUCK)
            }

            AudioManager.AUDIOFOCUS_LOSS                    -> {
                // Lost focus for an unbounded amount of time: stop playback and release media playback
                controller.pause(releaseResource = true)
                controller.pauseReason = PAUSE_FOR_LOSS_OF_FOCUS
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT          -> {
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media playback because playback
                // is likely to resume
                if (controller.isPlaying()) {
                    controller.pause(releaseResource = false)
                    controller.pauseReason = PAUSE_FOR_TRANSIENT_LOSS_OF_FOCUS
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                controller.handler.removeMessages(UNDUCK)
                controller.handler.sendEmptyMessage(DUCK)
            }
        }
    }
}
