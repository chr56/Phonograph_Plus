/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.mediastore.sort

data class FileSortMode(val sortRef: SortRef, val revert: Boolean = false) {
    companion object {
        fun deserialize(str: String): FileSortMode {
            val array = str.split(':')
            return if (array.size != 2) FileSortMode(SortRef.ID) else
                FileSortMode(
                    SortRef.deserialize(array[0]), array[1] != "0"
                )
        }
    }

    fun serialize(): String =
        "${sortRef.serializedName}:${if (!revert) "0" else "1"}"
}
