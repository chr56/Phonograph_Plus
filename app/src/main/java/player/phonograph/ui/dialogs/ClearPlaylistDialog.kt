/*
 *  Copyright (c) 2022~2023 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistType
import player.phonograph.model.playlist.ResettablePlaylist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.util.permissions.hasStorageWritePermission
import player.phonograph.util.text.ItemGroup
import player.phonograph.util.text.buildDeletionMessage
import util.phonograph.playlist.PlaylistsManager
import androidx.fragment.app.DialogFragment
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        val smartLists = grouped.getOrDefault(SMART_PLAYLIST, emptyList()).filterIsInstance<SmartPlaylist>()
        val filesLists = grouped.getOrDefault(FILE_PLAYLIST, emptyList()).filterIsInstance<FilePlaylist>()

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
                resources.getQuantityString(R.plurals.item_playlists, filesLists.size),
                filesLists.map { it.name }
            ),
            ItemGroup(
                resources.getQuantityString(R.plurals.item_playlists_generated, smartLists.size),
                smartLists.map { it.name }
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
                smartLists.forEach { playlist ->
                    if (playlist is ResettablePlaylist) playlist.clear(requireContext())
                }
                // files
                CoroutineScope(Dispatchers.Default).launch {
                    PlaylistsManager.deletePlaylistWithGuide(
                        fragmentActivity, filesLists
                    )
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
    }
}
