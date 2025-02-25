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
        val validResult = action.valid(audioFile)
        if (validResult == EditAction.ValidResult.Valid) {
            try {
                action.execute(audioFile)
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

    companion object {
        private const val HINT = "please check file or storage permission"
    }
}
