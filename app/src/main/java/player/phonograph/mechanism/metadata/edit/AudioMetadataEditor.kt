/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.metadata.edit

import player.phonograph.App
import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.foundation.notification.Notifications
import player.phonograph.mechanism.scanner.MediaStoreScanner
import player.phonograph.model.metadata.EditAction
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

abstract class AudioMetadataEditor(
    protected val songFiles: List<File>,
    protected val editRequest: List<EditAction>,
) {
    /**
     * execute [editRequest] on [songFiles]
     */
    suspend fun execute(context: Context) {
        if (editRequest.isEmpty()) return
        withContext(Dispatchers.Default) {
            val notifications = Notifications.BackgroundTasks.Default
            // notify user first
            notifications.post(
                context,
                title = App.instance.getString(R.string.action_tag_editor),
                msg = App.instance.getString(R.string.state_saving_changes),
                id = TAG_EDITOR_NOTIFICATION_CODE
            )
            // process
            withContext(Dispatchers.IO) {
                for (songFile in songFiles) {
                    if (songFile.canWrite()) {
                        applyEditActions(songFile, editRequest)
                    } else {
                        logs.add("Could not write file ${songFile.path}, please check file or storage permission")
                    }
                }
            }
            // notify user
            notifications.cancel(context, TAG_EDITOR_NOTIFICATION_CODE)
            if (logs.isNotEmpty()) warning(context, LOG_TAG, logs.joinToString(separator = "\n"))
            yield()
            // refresh media store
            val paths = songFiles.map { it.path }.toTypedArray()
            MediaStoreScanner(context).scan(paths)
        }
    }

    /**
     * actual execute [editRequest] on [songFiles]
     */
    abstract fun applyEditActions(file: File, requests: List<EditAction>)

    protected val logs: MutableList<String> = mutableListOf()

    companion object{
        private const val LOG_TAG = "AudioMetadataEditor"

        private const val TAG_EDITOR_NOTIFICATION_CODE = 824_3348
    }
}