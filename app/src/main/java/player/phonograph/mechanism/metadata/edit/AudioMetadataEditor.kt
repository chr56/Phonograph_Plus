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
import player.phonograph.App
import player.phonograph.R
import player.phonograph.mechanism.scanner.MediaStoreScanner
import player.phonograph.notification.BackgroundNotification
import player.phonograph.util.warning
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.IOException

class AudioMetadataEditor(
    private val songFiles: List<File>,
    private val editRequest: List<EditAction>,
) {

    private val logs: MutableList<String> = mutableListOf()

    suspend fun execute(context: Context) {
        if (editRequest.isEmpty()) return
        withContext(Dispatchers.Default) {
            // notify user first
            BackgroundNotification.post(
                App.instance.getString(R.string.action_tag_editor),
                App.instance.getString(R.string.saving_changes),
                TAG_EDITOR_NOTIFICATION_CODE
            )
            // process
            withContext(Dispatchers.IO) {
                for (songFile in songFiles) {
                    if (songFile.canWrite()) {
                        applyEditActions(songFile, editRequest)
                    } else {
                        logs.add("Could not write file ${songFile.path}, $HINT")
                    }
                }
            }
            // notify user
            BackgroundNotification.remove(TAG_EDITOR_NOTIFICATION_CODE)
            if (logs.isNotEmpty()) warning(LOG_TAG, logs.joinToString(separator = "\n"))
            yield()
            // refresh media store
            val paths = songFiles.map { it.path }.toTypedArray()
            MediaStoreScanner(context).scan(paths)
        }
    }

    private fun applyEditActions(file: File, requests: List<EditAction>) {
        safeEditTag(file.path) {
            val audioFile = AudioFileIO.read(file)
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
        private const val LOG_TAG = "MetadataEditor"
        private const val HINT = "please check file or storage permission"

        private const val TAG_EDITOR_NOTIFICATION_CODE = 824_3348
    }
}
