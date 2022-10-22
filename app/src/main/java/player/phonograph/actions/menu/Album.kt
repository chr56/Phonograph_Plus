/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions.menu

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.actions.fragmentActivity
import player.phonograph.actions.actionPlayQueue
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.model.Album
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.activities.AlbumDetailActivity
import player.phonograph.ui.dialogs.DeleteSongsDialog
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.NavigationUtil.goToArtist
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.AlbumTagEditorActivity

fun albumDetailToolbar(
    menu: Menu,
    context: Context,
    album: Album,
    @ColorInt iconColor: Int,
    loadWikiCallback: (Album) -> Boolean,
): Boolean = with(context) {
    attach(menu) {

        menuItem(title = getString(R.string.action_play)) { //id = R.id.action_shuffle_album
            icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick { album.songs.actionPlayQueue(context) }
        }

        menuItem(title = getString(R.string.action_shuffle_album)) { //id = R.id.action_shuffle_album
            icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                MusicPlayerRemote
                    .playQueue(album.songs, 0, true, ShuffleMode.SHUFFLE)
                true
            }
        }


        menuItem(title = getString(R.string.action_play_next)) { //id = R.id.action_play_next
            icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                MusicPlayerRemote.playNext(album.songs)
                true
            }
        }


        menuItem(title = getString(R.string.action_add_to_playing_queue)) { //id = R.id.action_add_to_current_playing
            icon = getTintedDrawable(R.drawable.ic_library_add_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                MusicPlayerRemote.enqueue(album.songs)
                true
            }
        }

        menuItem(title = getString(R.string.action_add_to_playlist)) { //id = R.id.action_add_to_playlist
            icon = getTintedDrawable(R.drawable.ic_playlist_add_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                fragmentActivity(context) {
                    AddToPlaylistDialog.create(album.songs).show(it.supportFragmentManager, "ADD_PLAYLIST")
                    true
                }
            }
        }

        menuItem(title = getString(R.string.action_go_to_artist)) { //id = R.id.action_go_to_artist
            icon = getTintedDrawable(R.drawable.ic_person_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                if (context is Activity)
                    goToArtist(context, album.artistId)
                true
            }
        }


        menuItem(title = getString(R.string.action_tag_editor)) { //id = R.id.action_tag_editor
            icon = getTintedDrawable(R.drawable.ic_library_music_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                if (context is ComponentActivity)
                    context.startActivityForResult(
                        Intent(context, AlbumTagEditorActivity::class.java).apply {
                            putExtra(AbsTagEditorActivity.EXTRA_ID, album.id)
                        },
                        AlbumDetailActivity.TAG_EDITOR_REQUEST
                    )
                true
            }
        }


        menuItem(title = getString(R.string.action_delete_from_device)) { //id = R.id.action_delete_from_device
            icon = getTintedDrawable(R.drawable.ic_delete_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                fragmentActivity(context) {
                    DeleteSongsDialog.create(ArrayList(album.songs)).show(it.supportFragmentManager, "ADD_PLAYLIST")
                    true
                }
            }
        }

        menuItem(title = getString(R.string.wiki)) { //id = R.id.action_wiki
            icon = getTintedDrawable(R.drawable.ic_info_outline_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                loadWikiCallback(album)
            }
        }
    }
    true
}