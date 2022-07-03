/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.helper.menu

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import player.phonograph.R
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.DeleteSongsDialog
import player.phonograph.dialogs.SongDetailDialog
import player.phonograph.interfaces.PaletteColorHolder
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.BlacklistUtil
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil
import player.phonograph.util.RingtoneManager
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.SongTagEditorActivity

/**
 * @param activity   The activity who has the menu that call this funcyion
 * @param song       Song to process
 * @param menuItemId ItemId in menu as well as `Unique Action ID`
 */
fun onSongMenuItemClick(activity: FragmentActivity, song: Song, menuItemId: Int): Boolean {
    when (menuItemId) {
        R.id.action_add_to_playlist -> {
            AddToPlaylistDialog.create(listOf(song))
                .show(activity.supportFragmentManager, "ADD_PLAYLIST")
            return true
        }
        R.id.action_play_next -> {
            MusicPlayerRemote.playNext(song)
            return true
        }
        R.id.action_add_to_current_playing -> {
            MusicPlayerRemote.enqueue(song)
            return true
        }
        R.id.action_tag_editor -> {
            val tagEditorIntent = Intent(activity, SongTagEditorActivity::class.java)
            tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_ID, song.id)
            if (activity is PaletteColorHolder) tagEditorIntent.putExtra(
                AbsTagEditorActivity.EXTRA_PALETTE,
                (activity as PaletteColorHolder).paletteColor
            )
            activity.startActivity(tagEditorIntent)
            return true
        }
        R.id.action_details -> {
            SongDetailDialog.create(song).show(activity.supportFragmentManager, "SONG_DETAILS")
            return true
        }
        R.id.action_add_to_black_list -> {
            BlacklistUtil.addToBlacklist(activity, song)
        }
        R.id.action_delete_from_device -> {
            DeleteSongsDialog.create(listOf(song))
                .show(activity.supportFragmentManager, "DELETE_SONGS")
            return true
        }
        R.id.action_go_to_album -> {
            NavigationUtil.goToAlbum(activity, song.albumId)
            return true
        }
        R.id.action_go_to_artist -> {
            NavigationUtil.goToArtist(activity, song.artistId)
            return true
        }
        R.id.action_set_as_ringtone -> {
            if (RingtoneManager.requiresDialog(activity)) {
                RingtoneManager.showDialog(activity)
            } else {
                RingtoneManager.setRingtone(activity, song.id)
            }
            return true
        }
        R.id.action_share -> {
            activity.startActivity(
                Intent.createChooser(
                    MusicUtil.createShareSongFileIntent(
                        song,
                        activity
                    ),
                    null
                )
            )
            return true
        }
    }
    return false
}