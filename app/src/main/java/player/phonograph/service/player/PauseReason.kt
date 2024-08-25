/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.service.player

import androidx.annotation.IntDef

@IntDef(
    PauseReason.NOT_PAUSED,
    PauseReason.PAUSE_BY_MANUAL_ACTION,
    PauseReason.PAUSE_FOR_QUEUE_ENDED,
    PauseReason.PAUSE_FOR_AUDIO_BECOMING_NOISY,
    PauseReason.PAUSE_FOR_TRANSIENT_LOSS_OF_FOCUS,
    PauseReason.PAUSE_FOR_LOSS_OF_FOCUS,
    PauseReason.PAUSE_ERROR
)
@Retention(AnnotationRetention.SOURCE)
annotation class PauseReason {
    companion object {
        const val NOT_PAUSED = 0
        const val PAUSE_BY_MANUAL_ACTION = 2
        const val PAUSE_FOR_QUEUE_ENDED = 4
        const val PAUSE_FOR_AUDIO_BECOMING_NOISY = 8
        const val PAUSE_FOR_TRANSIENT_LOSS_OF_FOCUS = 16
        const val PAUSE_FOR_LOSS_OF_FOCUS = 32
        const val PAUSE_ERROR = -2
    }
}