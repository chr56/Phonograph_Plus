/*
 *  Copyright (c) 2022~2023 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import lib.phonograph.misc.IOpenDirStorageAccess
import lib.phonograph.storage.getAbsolutePath
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.ResettablePlaylist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.util.coroutineToast
import player.phonograph.util.permissions.hasStorageWritePermission
import player.phonograph.util.reportError
import player.phonograph.util.sentPlaylistChangedLocalBoardCast
import player.phonograph.util.text.ItemGroup
import player.phonograph.util.text.buildDeletionMessage
import util.phonograph.playlist.mediastore.deletePlaylistsViaMediastore
import util.phonograph.playlist.saf.searchPlaylistsForDeletionViaSAF
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClearPlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlists: List<Playlist> = requireArguments().getParcelableArrayList(KEY)!!

        // classify
        val grouped = playlists.groupBy {
            when (it) {
                is SmartPlaylist -> SMART_PLAYLIST
                is FilePlaylist  -> FILE_PLAYLIST
                else             -> OTHER_PLAYLIST
            }
        }
        val smartPlaylists = grouped.getOrDefault(SMART_PLAYLIST, emptyList()).filterIsInstance<SmartPlaylist>()
        val filesPlaylists = grouped.getOrDefault(FILE_PLAYLIST, emptyList()).filterIsInstance<FilePlaylist>()

        // extra permission check on R(11)
        val hasPermission = hasStorageWritePermission(requireContext())

        // generate msg
        val msg = buildDeletionMessage(
            context = requireContext(),
            itemSize = playlists.size,
            extraSuffix = if (!hasPermission) requireContext().getString(
                R.string.permission_manage_external_storage_denied
            ) else "",
            ItemGroup(
                resources.getQuantityString(R.plurals.item_playlists, filesPlaylists.size, filesPlaylists.size),
                filesPlaylists.map { it.name }
            ),
            ItemGroup(
                resources.getQuantityString(R.plurals.item_playlists_generated, smartPlaylists.size, smartPlaylists.size),
                smartPlaylists.map { it.name }
            )
        )

        val fragmentActivity: Activity = requireActivity()
        // build dialog
        val dialog = MaterialDialog(requireActivity())
            .title(R.string.delete_action)
            .message(text = msg)
            .negativeButton(android.R.string.cancel) { dismiss() }
            .positiveButton(R.string.delete_action) {
                it.dismiss()
                // smart
                smartPlaylists.forEach { playlist ->
                    if (playlist is ResettablePlaylist) playlist.clear(requireContext())
                }
                // files
                CoroutineScope(Dispatchers.Default).launch {
                    deletePlaylistWithGuide(fragmentActivity, filesPlaylists)
                }
            }.also {
                // grant permission button for R
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasPermission) {
                    it.neutralButton(R.string.grant_permission) {
                        startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${requireContext().packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                }
                // set button color
                it.getActionButton(WhichButton.POSITIVE).updateTextColor(
                    ThemeColor.accentColor(requireContext())
                )
                it.getActionButton(WhichButton.NEGATIVE).updateTextColor(
                    ThemeColor.accentColor(requireContext())
                )
                it.getActionButton(WhichButton.NEUTRAL).updateTextColor(
                    ThemeColor.accentColor(requireContext())
                )
            }

        return dialog
    }

    companion object {
        private const val KEY = "playlists"
        private const val TAG = "ClearPlaylistDialog"

        fun create(playlists: List<Playlist>): ClearPlaylistDialog =
            ClearPlaylistDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(KEY, ArrayList(playlists))
                }
            }

        private const val SMART_PLAYLIST = 1 shl 1
        private const val FILE_PLAYLIST = 1 shl 2
        private const val OTHER_PLAYLIST = 1 shl 8

        /**
         * @param context must be IOpenDirStorageAccess
         */
        suspend fun deletePlaylistWithGuide(
            context: Context,
            filePlaylists: List<FilePlaylist>,
        ) = withContext(Dispatchers.IO) {
            // try to delete
            val failList = deletePlaylistsViaMediastore(context, filePlaylists)

            if (failList.isNotEmpty()) {

                // generate error msg

                val message = buildString {
                    appendLine(
                        context.resources.getQuantityString(
                            R.plurals.msg_deletion_result,
                            filePlaylists.size, filePlaylists.size - failList.size, filePlaylists.size
                        )
                    )
                    appendLine(
                        "${context.getString(R.string.failed_to_delete)}: "
                    )
                    for (playlist in failList) {
                        appendLine(playlist.name)
                    }
                }

                // report failure
                withContext(Dispatchers.Main) {
                    MaterialDialog(context)
                        .title(R.string.failed_to_delete)
                        .message(text = message)
                        .positiveButton(android.R.string.ok)
                        .negativeButton(R.string.delete_with_saf) {
                            CoroutineScope(Dispatchers.IO).launch {
                                if (context is Activity && context is IOpenDirStorageAccess) {
                                    deletePlaylistsViaSAF(context, filePlaylists)
                                } else {
                                    coroutineToast(context, R.string.failed)
                                }
                            }
                        }
                        .also {
                            // color
                            it.getActionButton(WhichButton.POSITIVE)
                                .updateTextColor(ThemeColor.accentColor(context))
                            it.getActionButton(WhichButton.NEGATIVE)
                                .updateTextColor(ThemeColor.accentColor(context))
                            it.getActionButton(WhichButton.NEUTRAL)
                                .updateTextColor(ThemeColor.accentColor(context))
                        }
                        .show()
                }
            }
        }

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
            val files = searchPlaylistsForDeletionViaSAF(activity, filePlaylists)

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
                        ?.setTextColor(activity.getColor(mt.color.R.color.md_red_800))
                    it.getButton(DialogInterface.BUTTON_NEGATIVE)
                        ?.setTextColor(activity.getColor(mt.color.R.color.md_grey_500))
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
    }
}
