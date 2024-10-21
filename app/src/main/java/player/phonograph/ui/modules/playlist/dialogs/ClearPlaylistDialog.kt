/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.playlist.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import lib.storage.documentProviderUriAbsolutePath
import lib.storage.launcher.IOpenDirStorageAccessible
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistManager
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.playlist.Playlist
import player.phonograph.util.coroutineToast
import player.phonograph.util.file.selectDocumentUris
import player.phonograph.util.parcelableArrayList
import player.phonograph.util.permissions.StoragePermissionChecker
import player.phonograph.util.reportError
import player.phonograph.util.sentPlaylistChangedLocalBoardCast
import player.phonograph.util.text.ItemGroup
import player.phonograph.util.text.buildDeletionMessage
import player.phonograph.util.theme.tintButtons
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClearPlaylistDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireActivity()
        val playlists: List<Playlist> = requireArguments().parcelableArrayList(EXTRA_PLAYLIST)!!

        // extra permission check on R(11)
        val hasPermission = StoragePermissionChecker.hasStorageWritePermission(context)

        // generate msg
        val message = buildDeletionMessage(
            context = context,
            itemSize = playlists.size,
            extraSuffix = if (!hasPermission) context.getString(R.string.permission_manage_external_storage_denied) else "",
            ItemGroup(
                resources.getQuantityString(R.plurals.item_playlists, playlists.size, playlists.size),
                playlists.map { playlist ->
                    "${playlist.name} (${playlist.location.text(context)})"
                }
            ),
        )

        // build dialog
        val dialog = MaterialDialog(requireActivity())
            .title(R.string.delete_action)
            .message(text = message)
            .noAutoDismiss()
            .negativeButton(android.R.string.cancel) { dismiss() }
            .positiveButton(R.string.delete_action) {
                CoroutineScope(Dispatchers.IO).launch {
                    delete(context, playlists)
                }
                it.dismiss()
            }.also {
                // grant permission button for R
                if (SDK_INT >= VERSION_CODES.R && !hasPermission) {
                    @Suppress("DEPRECATION")
                    it.neutralButton(R.string.grant_permission) {
                        startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                }
            }
            .tintButtons()

        return dialog
    }

    /**
     * @param context must be IOpenDirStorageAccess
     */
    private suspend fun delete(context: FragmentActivity, playlists: List<Playlist>) {

        /* Normally Delete (MediaStore + Internal database) */
        val results = playlists.map { playlist ->
            PlaylistManager.delete(playlist, false).delete(context)
        }

        /* Check */
        val allCount = results.size
        val failureCount = results.count { !it }
        val failures = results.mapIndexedNotNull { index, result -> if (!result) playlists[index] else null }
        val errorMessages = buildString {
            appendLine(
                context.resources.getQuantityString(
                    R.plurals.msg_deletion_result,
                    allCount, allCount - failureCount, allCount
                )
            )
            if (failureCount > 0) {
                appendLine(
                    "${context.getString(R.string.failed_to_delete)}: "
                )
                for (failure in failures) {
                    appendLine("${failure.name}(${failure.location})")
                }
            }
        }

        /* Again */
        withContext(Dispatchers.Main) {
            MaterialDialog(context)
                .title(R.string.action_delete_from_device)
                .message(text = errorMessages)
                .positiveButton(android.R.string.ok)
                .apply {
                    if (failureCount > 0) negativeButton(R.string.delete_with_saf) {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (context is IOpenDirStorageAccessible) {
                                deleteViaSAF(context, failures)
                            } else {
                                coroutineToast(context, R.string.failed)
                            }
                        }
                    }
                }
                .tintButtons()
                .show()
        }
    }
    /**
     * use SAF to choose a directory, and delete playlist inside this directory with user's confirmation
     * @param activity must be [IOpenDirStorageAccessible]
     * @param playlists playlists to delete
     */
    private suspend fun deleteViaSAF(activity: Activity, playlists: List<Playlist>) {
        require(activity is IOpenDirStorageAccessible)

        val paths = playlists.mapNotNull { playlist -> playlist.path() }
        val uris = selectDocumentUris(activity, paths)

        val warnings = buildDeletionMessage(
            context = activity,
            itemSize = uris.size,
            "",
            ItemGroup(
                activity.resources.getQuantityString(R.plurals.item_files, playlists.size, playlists.size),
                uris.mapNotNull { uri -> documentProviderUriAbsolutePath(uri, activity) ?: uri.path }
            )
        )

        withContext(Dispatchers.Main) {
            val dialog = AlertDialog.Builder(activity)
                .setTitle(R.string.delete_action)
                .setMessage(warnings)
                .setPositiveButton(R.string.delete_action) { dialog, _ ->
                    val failed = deleteUri(activity, uris)
                    sentPlaylistChangedLocalBoardCast()
                    dialog.dismiss()
                    if (failed.isNotEmpty()) {
                        val msg = failed.fold("Failed to delete: ") { acc, uri ->
                            val absolutePath = documentProviderUriAbsolutePath(uri, activity) ?: uri.path
                            "$acc, $absolutePath"
                        }
                        reportError(Exception(msg), TAG, msg)
                    }
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .create().tintButtons()

            dialog.also {
                it.getButton(DialogInterface.BUTTON_POSITIVE)
                    ?.setTextColor(activity.getColor(util.theme.materials.R.color.md_red_800))
                it.getButton(DialogInterface.BUTTON_NEGATIVE)
                    ?.setTextColor(activity.getColor(util.theme.materials.R.color.md_grey_500))
            }

            dialog.show()
        }
    }
    /**
     * Delete Document Uri
     * @return failed list
     */
    private fun deleteUri(context: Context, uris: Collection<Uri>): Collection<Uri> {
        return uris.mapNotNull { uri ->
            if (!DocumentsContract.deleteDocument(context.contentResolver, uri)) {
                uri
            } else {
                null
            }
        }
    }

    companion object {
        private const val EXTRA_PLAYLIST = "playlists"
        private const val TAG = "ClearPlaylistDialog"

        fun create(playlists: Collection<Playlist>): ClearPlaylistDialog =
            ClearPlaylistDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(EXTRA_PLAYLIST, ArrayList(playlists))
                }
            }
    }
}
