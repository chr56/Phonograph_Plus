/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagNotFoundException

sealed interface EditAction {

    val key: FieldKey

    val description: String

    fun valid(audioFile: AudioFile): ValidResult

    data class Delete(override val key: FieldKey) : EditAction {
        override val description: String get() = "Delete $key"
        override fun valid(audioFile: AudioFile): ValidResult {
            val tag = audioFile.tag ?: return ValidResult.NoSuchKey
            val target = try {
                tag.getFirst(key)
            } catch (e: TagNotFoundException) {
                null
            }
            return when (target) {
                null -> ValidResult.NoSuchKey
                else -> ValidResult.Valid
            }
        }
    }

    data class Update(override val key: FieldKey, val newValue: String) : EditAction {
        override val description: String get() = "Update $key to $newValue"
        override fun valid(audioFile: AudioFile): ValidResult {
            val tag = audioFile.tag ?: return ValidResult.NoSuchKey
            val target = try {
                tag.getFirst(key)
            } catch (e: TagNotFoundException) {
                null
            }
            return when (target) {
                null     -> ValidResult.NoSuchKey
                newValue -> ValidResult.NoChange
                else     -> ValidResult.Valid
            }
        }
    }

    data class Insert(override val key: FieldKey, val value: String) : EditAction {
        override val description: String get() = "Insert $key as $value"
        override fun valid(audioFile: AudioFile): ValidResult {
            val tag = audioFile.tag ?: return ValidResult.NoSuchKey
            val target = try {
                tag.getFirst(key)
            } catch (e: TagNotFoundException) {
                null
            }
            return when (target) {
                null -> ValidResult.Valid
                else -> ValidResult.AlreadyExisted
            }
        }
    }


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

    companion object {
        private const val TAG = "EditAction"
    }

}