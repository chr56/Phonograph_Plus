package player.phonograph.service.player

import player.phonograph.service.player.PlayerController.Companion.PAUSE_FOR_LOSS_OF_FOCUS
import player.phonograph.service.player.PlayerController.Companion.PAUSE_FOR_TRANSIENT_LOSS_OF_FOCUS
import player.phonograph.service.player.PlayerController.ControllerHandler.Companion.DUCK
import player.phonograph.service.player.PlayerController.ControllerHandler.Companion.UNDUCK
import androidx.annotation.RequiresApi
import android.content.Context.AUDIO_SERVICE
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import kotlin.LazyThreadSafetyMode.NONE

/**
 * @author chr_56 & Abou Zeid (kabouzeid) (original author)
 */
@Suppress("DEPRECATION")
class AudioFocusManager(private val controller: PlayerController) {

    private val audioManager: AudioManager by lazy(NONE) {
        controller.service.getSystemService(AUDIO_SERVICE) as AudioManager
    }

    fun requestAudioFocus(): Boolean {
        val result = if (SDK_INT >= O) {
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        return result == AudioManager.AUDIOFOCUS_GAIN
    }

    fun abandonAudioFocus() {
        if (SDK_INT >= O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    @delegate:RequiresApi(O)
    private val audioFocusRequest: AudioFocusRequest by lazy(NONE) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(audioFocusListener)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .build()
    }


    private val audioFocusListener: AudioManager.OnAudioFocusChangeListener by lazy(NONE) {
        AudioManager.OnAudioFocusChangeListener { focusChange ->
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
                    controller.pause()
                    controller.pauseReason = PAUSE_FOR_LOSS_OF_FOCUS
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT          -> {
                    // Lost focus for a short time, but we have to stop
                    // playback. We don't release the media playback because playback
                    // is likely to resume
                    if (controller.isPlaying()) {
                        controller.pause()
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
}
