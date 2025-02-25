/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.metadata.edit

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.KeyNotFoundException
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.tag.TagNotFoundException
import org.jaudiotagger.tag.images.AndroidArtwork
import player.phonograph.mechanism.metadata.JAudioTaggerMetadataKeyTranslator.toFieldKey
import player.phonograph.model.metadata.EditAction
import player.phonograph.model.metadata.EditAction.Executor
import player.phonograph.model.metadata.EditAction.Executor.ValidResult
import android.util.Log
import java.io.File
import java.io.IOException

class JAudioTaggerAudioMetadataEditor(songFiles: List<File>, editRequest: List<EditAction>) :
        AudioMetadataEditor(songFiles, editRequest) {

    override fun applyEditActions(file: File, requests: List<EditAction>) {
        safeEditTag(file.path) {
            val audioFile = readAudioFile(file) ?: return
            for (action in requests) {
                applyEditAction(audioFile, action)
            }
            audioFile.commit()
        }
    }

    private fun applyEditAction(audioFile: AudioFile, action: EditAction) {
        val executor = executorFor(audioFile, action)
        val validResult = executor.valid()
        if (validResult == ValidResult.Valid) {
            try {
                executor.execute()
            } catch (e: KeyNotFoundException) {
                logs.add("Unknown FieldKey: ${action.key} \n${summaryThrowable(e)}")
            } catch (e: TagException) {
                logs.add("Failed to execute step: ${action.description} \n${summaryThrowable(e)}")
            }
        } else {
            logs.add(
                "Failed to execute step action(${action.description}) due to [${validResult.message}], ignored!"
            )
        }
    }

    private fun readAudioFile(file: File): AudioFile? = try {
        if (file.extension.isNotEmpty()) {
            AudioFileIO.read(file)
        } else {
            AudioFileIO.readMagic(file)
        }
    } catch (e: CannotReadException) {
        logs.add("Failed to read file, $HINT! \n${summaryThrowable(e)}")
        null
    }


    //region Executors

    private fun executorFor(file: AudioFile, action: EditAction): Executor = when (action) {
        is EditAction.Delete       -> DeleteExecutor(file, action)
        is EditAction.ImageDelete  -> ImageDeleteExecutor(file, action)
        is EditAction.ImageReplace -> ImageReplaceExecutor(file, action)
        is EditAction.Update       -> UpdateExecutor(file, action)
    }

    class DeleteExecutor(val audioFile: AudioFile, val action: EditAction.Delete) : Executor {
        override fun valid(): ValidResult {
            val tag = audioFile.tag ?: return ValidResult.NoSuchKey
            val target = try {
                tag.getFirst(action.key.toFieldKey())
            } catch (e: TagNotFoundException) {
                null
            }
            return when (target) {
                null -> ValidResult.NoSuchKey
                else -> ValidResult.Valid
            }
        }

        override fun execute() {
            audioFile.tagOrCreateAndSetDefault.deleteField(action.key.toFieldKey())
        }
    }

    class ImageDeleteExecutor(val audioFile: AudioFile, val action: EditAction.ImageDelete) : Executor {
        override fun valid(): ValidResult {
            val size = audioFile.tag?.artworkList?.size ?: 0
            return if (size > 0) ValidResult.Valid else ValidResult.NoSuchKey
        }

        override fun execute() {
            audioFile.tagOrCreateAndSetDefault.deleteArtworkField()
        }
    }

    class UpdateExecutor(val audioFile: AudioFile, val action: EditAction.Update) : Executor {
        override fun valid(): ValidResult {
            val tag = audioFile.tag ?: return ValidResult.NoSuchKey
            val target = try {
                tag.getFirst(action.key.toFieldKey())
            } catch (e: TagNotFoundException) {
                null
            }
            return when (target) {
                null            -> ValidResult.NoSuchKey
                action.newValue -> ValidResult.NoChange
                else            -> ValidResult.Valid
            }
        }

        override fun execute() {
            audioFile.tagOrCreateAndSetDefault.setField(action.key.toFieldKey(), action.newValue)
        }
    }

    class ImageReplaceExecutor(val audioFile: AudioFile, val action: EditAction.ImageReplace) : Executor {
        override fun valid(): ValidResult {
            return ValidResult.Valid
        }

        override fun execute() {
            audioFile.tagOrCreateAndSetDefault.addField(AndroidArtwork.createArtworkFromFile(action.file))
        }
    }
    //endregion

    //region Utils
    private inline fun safeEditTag(path: String, block: () -> Unit) {
        try {
            block()
        } catch (e: CannotReadException) {
            logs.add("Failed to read file, $HINT! \n${summaryThrowable(e)}")
        } catch (e: CannotWriteException) {
            logs.add("Failed to write file, $HINT! \n${summaryThrowable(e)}")
        } catch (e: IOException) {
            logs.add("IO error, $HINT! \n${summaryThrowable(e)}")
        } catch (e: ReadOnlyFileException) {
            logs.add("File is read only: $path! \n${summaryThrowable(e)}")
        } catch (e: InvalidAudioFrameException) {
            logs.add("File maybe corrupted, $HINT! \n${summaryThrowable(e)}")
        } catch (e: TagException) {
            logs.add("Tag(s) may have glitches, $HINT! \n${summaryThrowable(e)}")
        }
    }

    private fun summaryThrowable(throwable: Throwable): String =
        "${throwable.javaClass.name}: ${throwable.message}:\n${Log.getStackTraceString(throwable)}"
    //endregion

    companion object {
        private const val HINT = "please check file or storage permission"
    }
}
