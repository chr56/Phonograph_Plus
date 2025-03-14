/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.service

enum class ShuffleMode(private val serialNum: Int) {
    NONE(SHUFFLE_MODE_NONE),
    SHUFFLE(SHUFFLE_MODE_SHUFFLE);

    fun serialize(): Int = serialNum

    companion object {
        fun deserialize(n: Int): ShuffleMode {
            return when (n) {
                SHUFFLE_MODE_SHUFFLE -> SHUFFLE
                SHUFFLE_MODE_NONE    -> NONE
                else                 -> throw IllegalStateException("invalid shuffle mode")
            }
        }
    }
}

const val SHUFFLE_MODE_NONE = 0
const val SHUFFLE_MODE_SHUFFLE = 1
