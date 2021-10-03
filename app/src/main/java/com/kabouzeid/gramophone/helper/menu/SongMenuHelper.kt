package com.kabouzeid.gramophone.helper.menu

import android.content.Intent
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.dialogs.AddToPlaylistDialog
import com.kabouzeid.gramophone.dialogs.DeleteSongsDialog
import com.kabouzeid.gramophone.dialogs.SongDetailDialog
import com.kabouzeid.gramophone.helper.MusicPlayerRemote
import com.kabouzeid.gramophone.interfaces.PaletteColorHolder
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.ui.activities.tageditor.AbsTagEditorActivity
import com.kabouzeid.gramophone.ui.activities.tageditor.SongTagEditorActivity
import com.kabouzeid.gramophone.util.MusicUtil
import com.kabouzeid.gramophone.util.NavigationUtil
import com.kabouzeid.gramophone.util.RingtoneManager

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object SongMenuHelper {
    const val menuResDefault = R.menu.menu_item_song

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

    abstract class ClickMenuListener(private val activity: AppCompatActivity, @Nullable @MenuRes resToUse: Int? = menuResDefault) :
        View.OnClickListener,
        PopupMenu.OnMenuItemClickListener {

        abstract val song: Song
        protected open var menuRes = menuResDefault // if resToUse is null

        init {
            resToUse?.let {
                menuRes = resToUse
            }
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
            return handleMenuClick(activity, song, item.itemId)
        }
    }
}
