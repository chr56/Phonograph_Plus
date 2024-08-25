/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.service.player

import androidx.annotation.IntDef

const val NOT_PAUSED = 0
const val PAUSE_BY_MANUAL_ACTION = 2
const val PAUSE_FOR_QUEUE_ENDED = 4
const val PAUSE_FOR_AUDIO_BECOMING_NOISY = 8
const val PAUSE_FOR_TRANSIENT_LOSS_OF_FOCUS = 16
const val PAUSE_FOR_LOSS_OF_FOCUS = 32
const val PAUSE_ERROR = -2

@IntDef(
    NOT_PAUSED,
    PAUSE_BY_MANUAL_ACTION,
    PAUSE_FOR_QUEUE_ENDED,
    PAUSE_FOR_AUDIO_BECOMING_NOISY,
    PAUSE_FOR_TRANSIENT_LOSS_OF_FOCUS,
    PAUSE_FOR_LOSS_OF_FOCUS,
    PAUSE_ERROR
)
@Retention(AnnotationRetention.SOURCE)
annotation class PauseReasonInt