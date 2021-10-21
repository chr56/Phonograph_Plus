package com.kabouzeid.phonograph.helper.menu

import android.content.Intent
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.kabouzeid.phonograph.R
import com.kabouzeid.phonograph.dialogs.AddToPlaylistDialog
import com.kabouzeid.phonograph.dialogs.DeleteSongsDialog
import com.kabouzeid.phonograph.dialogs.SongDetailDialog
import com.kabouzeid.phonograph.helper.MusicPlayerRemote
import com.kabouzeid.phonograph.interfaces.PaletteColorHolder
import com.kabouzeid.phonograph.model.Song
import com.kabouzeid.phonograph.ui.activities.tageditor.AbsTagEditorActivity
import com.kabouzeid.phonograph.ui.activities.tageditor.SongTagEditorActivity
import com.kabouzeid.phonograph.util.MusicUtil
import com.kabouzeid.phonograph.util.NavigationUtil
import com.kabouzeid.phonograph.util.RingtoneManager

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object SongMenuHelper {
    const val menuResDefault = R.menu.menu_item_song_short

    @JvmStatic
    fun handleMenuClick(activity: FragmentActivity, song: Song, menuItemId: Int): Boolean {
        when (menuItemId) {
            R.id.action_set_as_ringtone -> {
                if (RingtoneManager.requiresDialog(activity)) {
                    RingtoneManager.showDialog(activity)
                } else {
                    val ringtoneManager = RingtoneManager()
                    ringtoneManager.setRingtone(activity, song.id)
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
            R.id.action_delete_from_device -> {
                DeleteSongsDialog.create(listOf(song))
                    .show(activity.supportFragmentManager, "DELETE_SONGS")
                return true
            }
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
            R.id.action_go_to_album -> {
                NavigationUtil.goToAlbum(activity, song.albumId)
                return true
            }
            R.id.action_go_to_artist -> {
                NavigationUtil.goToArtist(activity, song.artistId)
                return true
            }
        }
        return false
    }

    abstract class ClickMenuListener(private val activity: AppCompatActivity, @Nullable @MenuRes resToUse: Int?) :
        View.OnClickListener,
        PopupMenu.OnMenuItemClickListener {

        abstract val song: Song
        protected open var menuRes = menuResDefault // defaultRes

        init {
            // as we all know, this is executed before onClick(v)
            resToUse?.let {
                menuRes = resToUse
            } // or the defaultRes
        }

        override fun onClick(v: View) {
            handleMenuButtonClick(v)
        }
        protected fun handleMenuButtonClick(v: View) {
            val popupMenu = PopupMenu(activity, v)
            popupMenu.inflate(menuRes)
            popupMenu.setOnMenuItemClickListener(this)
            popupMenu.show()
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            return SongMenuHelper.handleMenuClick(activity, song, item.itemId)
        }
    }
}
