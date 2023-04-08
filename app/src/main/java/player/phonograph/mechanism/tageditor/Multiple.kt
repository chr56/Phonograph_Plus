/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.tageditor

import org.jaudiotagger.tag.FieldKey
import player.phonograph.App
import player.phonograph.R
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.notification.BackgroundNotification
import android.app.Activity
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

fun applyEdit(
    scope: CoroutineScope,
    context: Context,
    songFiles: List<File>,
    allEditRequest: Map<FieldKey, String?>,
    needDeleteCover: Boolean,
    needReplaceCover: Boolean,
    newCoverUri: Uri?
) {
    scope.launch(Dispatchers.Default) {
        // notify user first
        BackgroundNotification.post(
            App.instance.getString(R.string.action_tag_editor),
            App.instance.getString(R.string.saving_changes),
            TAG_EDITOR_NOTIFICATION_CODE
        )
        // process
        withContext(Dispatchers.IO) {
            for (songFile in songFiles) {
                applyEditImpl(
                    context,
                    songFile,
                    allEditRequest,
                    needDeleteCover,
                    needReplaceCover,
                    newCoverUri
                )
            }
        }
        // notify user
        BackgroundNotification.remove(TAG_EDITOR_NOTIFICATION_CODE)
        // refresh media store
        val paths = songFiles.map { it.path }.toTypedArray()
        val listener =
            if (context is Activity)
                UpdateToastMediaScannerCompletionListener(
                    context,
                    paths
                ) else null
        yield()
        MediaScannerConnection.scanFile(
            App.instance, paths, null, listener
        )
    }
}

private const val TAG_EDITOR_NOTIFICATION_CODE = 824_3348_6