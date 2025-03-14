/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.service

enum class RepeatMode(private val serialNum: Int) {
    NONE(REPEAT_MODE_NONE),
    REPEAT_QUEUE(REPEAT_MODE_ALL),
    REPEAT_SINGLE_SONG(REPEAT_MODE_THIS);

    fun serialize(): Int = serialNum

    companion object {
        fun deserialize(n: Int): RepeatMode {
            return when (n) {
                REPEAT_MODE_NONE -> NONE
                REPEAT_MODE_ALL  -> REPEAT_QUEUE
                REPEAT_MODE_THIS -> REPEAT_SINGLE_SONG
                else             -> throw IllegalStateException("invalid repeat mode")
            }
        }
    }
}

const val REPEAT_MODE_NONE = 0
const val REPEAT_MODE_ALL = 1
const val REPEAT_MODE_THIS = 2