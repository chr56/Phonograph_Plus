/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.tageditor

import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.KeyNotFoundException
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagException
import player.phonograph.App
import player.phonograph.R
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.notification.BackgroundNotification
import player.phonograph.ui.compose.tag.EditRequestModel
import player.phonograph.util.Util.reportError
import android.app.Activity
import android.content.Context
import android.media.MediaScannerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.IOException

fun applyTagEdit(
    scope: CoroutineScope,
    context: Context,
    editRequestModel: EditRequestModel,
    songFile: File
) {
    scope.launch(Dispatchers.Default) {
        // notify user first
        BackgroundNotification.post(
            App.instance.getString(R.string.action_tag_editor),
            App.instance.getString(R.string.saving_changes),
            TAG_EDITOR_NOTIFICATION_CODE
        )
        // process
        val requests = editRequestModel.allRequests
        if (requests.isEmpty()) return@launch
        withContext(Dispatchers.IO) {
            applyTagEditImpl(context, songFile, requests)
        }
        // notify user
        BackgroundNotification.remove(TAG_EDITOR_NOTIFICATION_CODE)
        // refresh media store
        val listener =
            if (context is Activity)
                UpdateToastMediaScannerCompletionListener(
                    context,
                    arrayOf(songFile.path)
                ) else null
        yield()
        MediaScannerConnection.scanFile(
            App.instance, arrayOf(songFile.path), null, listener
        )
    }
}

private const val TAG_EDITOR_NOTIFICATION_CODE = 824_3348


private fun applyTagEditImpl(context: Context, songFile: File, requests: Map<FieldKey, String?>) {
    try {
        val file = AudioFileIO.read(songFile)
        writeTags(file, requests)
        file.commit()
    } catch (e: CannotReadException) {
        e.report("Failed to read file, $HINT!")
    } catch (e: CannotWriteException) {
        e.report("Failed to write file, $HINT!")
    } catch (e: IOException) {
        e.report("IO error, $HINT!")
    } catch (e: ReadOnlyFileException) {
        e.report("File is read only : ${songFile.path}.")
    } catch (e: InvalidAudioFrameException) {
        e.report("File maybe corrupted, $HINT!")
    } catch (e: TagException) {
        e.report("Tag(s) may have glitches, $HINT!")
    }
}

private fun writeTags(file: AudioFile, requests: Map<FieldKey, String?>) {
    val tagsHeader = file.tagOrCreateAndSetDefault
    for ((tagKey, value) in requests) {
        writeTag(tagsHeader, tagKey, value)
    }
}

private fun writeTag(tagsHeader: Tag, tagKey: FieldKey, value: String?) {
    try {
        if (value.isNullOrEmpty()) {
            tagsHeader.deleteField(tagKey)
        } else {
            tagsHeader.setField(tagKey, value)
        }
    } catch (e: KeyNotFoundException) {
        e.report("Unknown FieldKey: $tagKey")
    } catch (e: TagException) {
        e.report("Failed to save tag: $tagKey --> $$value")
    }
}

private fun Exception.report(message: String) = reportError(this, LOGTAG, message)

private const val HINT = "please check file or storage permission"
private const val LOGTAG = "TagEdit"
