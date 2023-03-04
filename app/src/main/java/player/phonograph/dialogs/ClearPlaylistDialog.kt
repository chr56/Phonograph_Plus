/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.ResettablePlaylist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.util.StringUtil
import player.phonograph.util.permissions.hasStorageWritePermission
import util.phonograph.m3u.PlaylistsManager
import androidx.fragment.app.DialogFragment
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings

class ClearPlaylistDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playlists: List<Playlist> = requireArguments().getParcelableArrayList(KEY)!!

        // classify
        val smartLists = ArrayList<SmartPlaylist>()
        val filesLists = ArrayList<FilePlaylist>()
        for (playlist in playlists) {
            when (playlist) {
                is FilePlaylist -> {
                    filesLists.add(playlist)
                }
                is SmartPlaylist -> {
                    if (playlist is ResettablePlaylist) smartLists.add(playlist)
                }
            }
        }

        // extra permission check on R(11)
        val hasPermission = hasStorageWritePermission(requireContext())

        // generate msg
        val msg = StringUtil.buildDeletionMessage(
            context = requireContext(),
            itemSize = playlists.size,
            extraSuffix = if (!hasPermission) requireContext().getString(
                R.string.permission_manage_external_storage_denied
            ) else "",
            StringUtil.ItemGroup(
                resources.getQuantityString(R.plurals.item_playlists, filesLists.size),
                filesLists.map { it.name }
            ),
            StringUtil.ItemGroup(
                resources.getQuantityString(R.plurals.item_playlists_generated, smartLists.size),
                smartLists.map { it.name }
            )
        )

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
                val attachedActivity: Activity = requireActivity()
                PlaylistsManager.deletePlaylistWithGuide(
                    attachedActivity, filesLists
                )
            }.also {
                // grant permission button for R
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasPermission) {
                    it.neutralButton(R.string.grant_permission) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${requireContext().packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
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

        @JvmStatic
        fun create(playlists: List<Playlist>): ClearPlaylistDialog =
            ClearPlaylistDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(
                        "playlists",
                        ArrayList(playlists)
                    )
                }
            }
    }
}
