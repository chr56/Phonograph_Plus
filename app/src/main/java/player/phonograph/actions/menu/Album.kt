/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions.menu

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.actions.actionAddToPlaylist
import player.phonograph.actions.actionDelete
import player.phonograph.actions.actionEnqueue
import player.phonograph.actions.actionPlay
import player.phonograph.actions.actionPlayNext
import player.phonograph.actions.activity
import player.phonograph.actions.fragmentActivity
import player.phonograph.model.Album
import player.phonograph.service.queue.ShuffleMode.NONE
import player.phonograph.service.queue.ShuffleMode.SHUFFLE
import player.phonograph.ui.compose.tag.BatchTagEditorActivity
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.NavigationUtil.goToArtist
import androidx.annotation.ColorInt
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import kotlin.random.Random

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
            onClick { album.songs.actionPlay(NONE, 0) }
        }

        menuItem(title = getString(R.string.action_shuffle_album)) { //id = R.id.action_shuffle_album
            icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick { album.songs.actionPlay(SHUFFLE, Random.nextInt(album.songs.size)) }
        }


        menuItem(title = getString(R.string.action_play_next)) { //id = R.id.action_play_next
            icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick { album.songs.actionPlayNext() }
        }


        menuItem(title = getString(R.string.action_add_to_playing_queue)) { //id = R.id.action_add_to_current_playing
            icon = getTintedDrawable(R.drawable.ic_library_add_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick { album.songs.actionEnqueue() }
        }

        menuItem(title = getString(R.string.action_add_to_playlist)) { //id = R.id.action_add_to_playlist
            icon = getTintedDrawable(R.drawable.ic_playlist_add_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick { album.songs.actionAddToPlaylist(context) }
        }

        menuItem(title = getString(R.string.action_go_to_artist)) { //id = R.id.action_go_to_artist
            icon = getTintedDrawable(R.drawable.ic_person_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                goToArtist(context, album.artistId)
                true
            }
        }


        menuItem(title = getString(R.string.action_tag_editor)) { //id = R.id.action_tag_editor
            icon = getTintedDrawable(R.drawable.ic_library_music_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                BatchTagEditorActivity.launch(context, ArrayList(album.songs))
                true
            }
        }


        menuItem(title = getString(R.string.action_delete_from_device)) { //id = R.id.action_delete_from_device
            icon = getTintedDrawable(R.drawable.ic_delete_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                fragmentActivity(context) {
                    album.songs.actionDelete(it)
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