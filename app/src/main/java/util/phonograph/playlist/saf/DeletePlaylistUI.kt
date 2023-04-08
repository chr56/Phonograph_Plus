/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.playlist.saf

import lib.phonograph.misc.IOpenDirStorageAccess
import lib.phonograph.storage.getAbsolutePath
import player.phonograph.R
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.util.Util.reportError
import player.phonograph.util.Util.sentPlaylistChangedLocalBoardCast
import player.phonograph.util.text.ItemGroup
import player.phonograph.util.text.buildDeletionMessage
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import android.app.Activity
import android.content.DialogInterface
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * use SAF to choose a directory, and delete playlist inside this directory with user's confirmation
 * @param activity must be [IOpenDirStorageAccess]
 * @param filePlaylists playlists to delete
 */
suspend fun deletePlaylistsViaSAF(
    activity: Activity,
    filePlaylists: List<FilePlaylist>,
) {
    require(activity is IOpenDirStorageAccess)
    val files = tryToDeletePlaylistsViaSAF(activity, filePlaylists)

    val message = buildDeletionMessage(
        context = activity,
        itemSize = files.size,
        null,
        ItemGroup(
            activity.resources.getQuantityString(R.plurals.item_files, filePlaylists.size),
            files.mapNotNull { file ->
                Log.v("FileDelete", "${file.name}@${file.uri}")
                file.getAbsolutePath(activity)
            }
        )
    )
    withContext(Dispatchers.Main) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.delete_action)
            .setMessage(message)
            .setPositiveButton(R.string.delete_action) { dialog, _ ->
                deleteFile(files)
                sentPlaylistChangedLocalBoardCast()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.also {
            it.getButton(DialogInterface.BUTTON_POSITIVE)
                .setTextColor(activity.getColor(R.color.md_red_800))
            it.getButton(DialogInterface.BUTTON_NEGATIVE)
                .setTextColor(activity.getColor(R.color.md_grey_500))
        }

        dialog.show()
    }

}

private fun deleteFile(files: Collection<DocumentFile>) {
    val failed = mutableListOf<DocumentFile>()
    for (file in files) {
        val result = file.delete()
        if (!result) failed.add(file)
    }
    if (failed.isNotEmpty()) {
        val msg = failed.fold("Failed to delete: ") { acc, s -> "$acc, ${s.uri.path}" }
        reportError(Exception(msg), TAG, msg)
    }
}

private const val TAG = "DeleteUI"