/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.service.queue

import java.lang.IllegalStateException

enum class ShuffleMode(private val serialNum: Int) {
    NONE(SHUFFLE_MODE_NONE),
    SHUFFLE(SHUFFLE_MODE_SHUFFLE);

    fun serialize(): Int = serialNum

    companion object {
        fun deserialize(n: Int): ShuffleMode {
            return when (n) {
                SHUFFLE_MODE_SHUFFLE -> SHUFFLE
                SHUFFLE_MODE_NONE -> NONE
                else -> throw IllegalStateException("invalid shuffle mode")
            }
        }
    }
}

enum class RepeatMode(private val serialNum: Int) {
    NONE(REPEAT_MODE_NONE),
    REPEAT_QUEUE(REPEAT_MODE_ALL),
    REPEAT_SINGLE_SONG(REPEAT_MODE_THIS);

    fun serialize(): Int = serialNum
    companion object {
        fun deserialize(n: Int): RepeatMode {
            return when (n) {
                REPEAT_MODE_NONE -> NONE
                REPEAT_MODE_ALL -> REPEAT_QUEUE
                REPEAT_MODE_THIS -> REPEAT_SINGLE_SONG
                else -> throw IllegalStateException("invalid repeat mode")
            }
        }
    }
}

const val SHUFFLE_MODE_NONE = 0
const val SHUFFLE_MODE_SHUFFLE = 1

const val REPEAT_MODE_NONE = 0
const val REPEAT_MODE_ALL = 1
const val REPEAT_MODE_THIS = 2
