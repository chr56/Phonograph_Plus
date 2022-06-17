/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.mediastore.sort

data class FileSortMode(val sortRef: FileRef, val revert: Boolean = false) {
    companion object {
        fun deserialize(str: String): FileSortMode {
            val array = str.split(':')
            return if (array.size != 2) FileSortMode(FileRef.ID) else
                FileSortMode(
                    FileRef.deserialize(array[0]), array[1] != "0"
                )
        }
    }

    fun serialize(): String =
        "${sortRef.serializedName}:${if (!revert) "0" else "1"}"
}

enum class FileRef(val serializedName: String) {

    ID("id"),
    DISPLAY_NAME("display_name"),
    PATH("path"),
    SIZE("size"),
    ADDED_DATE("added_date"),
    MODIFIED_DATE("modified_date") ;

    companion object {
        fun deserialize(serializedName: String): FileRef {
            return when (serializedName) {
                "id" -> ID
                "display_name" -> DISPLAY_NAME
                "path" -> PATH
                "size" -> SIZE
                "added_date" -> ADDED_DATE
                "modified_date" -> MODIFIED_DATE
                else -> ID
            }
        }
    }
}
