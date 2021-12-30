package player.phonograph.helper.menu

import android.content.Intent
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import player.phonograph.App
import player.phonograph.R
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.DeleteSongsDialog
import player.phonograph.dialogs.SongDetailDialog
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.interfaces.PaletteColorHolder
import player.phonograph.model.Song
import player.phonograph.provider.BlacklistStore
import player.phonograph.ui.activities.tageditor.AbsTagEditorActivity
import player.phonograph.ui.activities.tageditor.SongTagEditorActivity
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil
import player.phonograph.util.RingtoneManager
import java.io.File

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
object SongMenuHelper {

    @JvmStatic
    fun handleMenuClick(activity: FragmentActivity, song: Song, menuItemId: Int): Boolean {
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
                // parent folder
                var path: String = song.data.dropLastWhile { it != '/' }.dropLast(1) // last char is '/'

                val candidatesPath = mutableListOf<String>()
                while (path.isNotEmpty()) {
                    if (path.endsWith("/emulated/0", true)
                        or path.endsWith("/emulated", true)
                        or path.endsWith("/storage", true)
                    ) break // no junk paths
                    candidatesPath.add(path)
                    path = path.dropLastWhile { it != '/' }.dropLast(1) // last char is '/'
                }

                MaterialDialog(activity)
                    .title(R.string.label_file_path)
                    .noAutoDismiss()
                    .listItemsSingleChoice(items = candidatesPath) { dialog, _, pathText ->
                        if (pathText.isNotBlank()) {
                            MaterialDialog(activity)
                                .title(R.string.add_blacklist)
                                .message(text = pathText)
                                .positiveButton(android.R.string.ok) {
                                    BlacklistStore.getInstance(App.instance).addPath(File(pathText as String))
                                    dialog.dismiss()
                                }
                                .negativeButton(android.R.string.cancel)
                                .show()
                        }
                    }
                    .show()
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
        }
        return false
    }

    const val menuResDefault = R.menu.menu_item_song

    abstract class ClickMenuListener(private val activity: AppCompatActivity, @MenuRes menuRes: Int?) : View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        abstract val song: Song
        protected open var realRes = menuRes ?: menuResDefault

        override fun onClick(v: View) {
            handleMenuButtonClick(v)
        }
        private fun handleMenuButtonClick(v: View) {
            val popupMenu = PopupMenu(activity, v)
            popupMenu.inflate(realRes)
            popupMenu.setOnMenuItemClickListener(this)
            popupMenu.show()
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            return handleMenuClick(activity, song, item.itemId)
        }
    }
}
