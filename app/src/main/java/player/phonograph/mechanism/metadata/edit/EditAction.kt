/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.metadata.edit

import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import java.io.File

sealed interface EditAction {

    val key: ConventionalMusicMetadataKey

    val description: String

    data class Delete(override val key: ConventionalMusicMetadataKey) : EditAction {
        override val description: String get() = "Delete $key"
    }

    object ImageDelete : EditAction {
        override val key: ConventionalMusicMetadataKey = ConventionalMusicMetadataKey.COVER_ART
        override val description: String get() = "Delete Cover Art"
    }

    data class Update(override val key: ConventionalMusicMetadataKey, val newValue: String) : EditAction {
        override val description: String get() = "Update $key to $newValue"
    }

    class ImageReplace(val file: File) : EditAction {
        override val key: ConventionalMusicMetadataKey = ConventionalMusicMetadataKey.COVER_ART
        override val description: String get() = "Replace Cover Art to ${file.path}"
    }


    interface Executor {

        fun valid(): ValidResult

        fun execute()

        sealed class ValidResult {
            abstract val message: String

            object Valid : ValidResult() {
                override val message: String get() = "Valid"
            }

            object NoChange : ValidResult() {
                override val message: String get() = "No changes are made"
            }

            object NoSuchKey : ValidResult() {
                override val message: String get() = "Key not found"
            }

            object AlreadyExisted : ValidResult() {
                override val message: String get() = "Already existed"
            }

            object ReadOnly : ValidResult() {
                override val message: String get() = "Read only file"
            }
        }
    }

    companion object {
        private const val TAG = "EditAction"

        /**
         * remove duplicated
         */
        fun merge(original: List<EditAction>): MutableList<EditAction> {
            val result = original.toMutableList()
            result.sortBy { it.key }
            var completed = false
            outer@ while (!completed) {
                var index = 0
                inner@ while (index + 1 <= result.lastIndex) {
                    val u = result[index]
                    val b = result[index + 1]
                    if (u.key == b.key) { // override check
                        result.removeAt(index)
                        continue@outer
                    }
                    index++
                }
                if (index + 1 >= result.lastIndex) completed = true
            }
            return result
        }
    }

}