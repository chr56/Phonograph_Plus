/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.model

@JvmInline
value class UIMode(val ordinal: Int) {
    companion object {
        val Common = UIMode(2)
        val Editor = UIMode(4)
        val Search = UIMode(8)
    }
}