/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.coil.CustomArtistImageStore
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.model.Artist
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.activities.ArtistDetailActivity
import player.phonograph.ui.dialogs.DeleteSongsDialog
import player.phonograph.util.ImageUtil.getTintedDrawable

fun applyToToolbar(
    menu: Menu,
    context: Context,
    artist: Artist,
    @ColorInt iconColor: Int,
    loadBiographyCallback: (Artist) -> Boolean,
): Boolean = with(context) {
    attach(menu) {

        menuItem(title = getString(R.string.action_play)) { //id = R.id.action_shuffle_artist
            icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                MusicPlayerRemote
                    .playQueueCautiously(artist.songs, 0, true, ShuffleMode.NONE)
                true
            }
        }

        menuItem(title = getString(R.string.action_shuffle_artist)) { //id = R.id.action_shuffle_artist
            icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                MusicPlayerRemote
                    .playQueue(artist.songs, 0, true, ShuffleMode.SHUFFLE)
                true
            }
        }


        menuItem(title = getString(R.string.action_play_next)) { //id = R.id.action_play_next
            icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                MusicPlayerRemote.playNext(artist.songs)
                true
            }
        }


        menuItem(title = getString(R.string.action_add_to_playing_queue)) { //id = R.id.action_add_to_current_playing
            icon = getTintedDrawable(R.drawable.ic_library_add_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                MusicPlayerRemote.enqueue(artist.songs)
                true
            }
        }

        menuItem(title = getString(R.string.action_add_to_playlist)) { //id = R.id.action_add_to_playlist
            icon = getTintedDrawable(R.drawable.ic_playlist_add_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                fragmentActivity(context) {
                    AddToPlaylistDialog.create(artist.songs).show(it.supportFragmentManager, "ADD_PLAYLIST")
                    true
                }
            }
        }

        menuItem(title = getString(R.string.set_artist_image)) { //id = R.id.action_set_artist_image
            icon = getTintedDrawable(R.drawable.ic_person_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                if (context is ComponentActivity)
                    context.startActivityForResult(
                        Intent.createChooser(
                            Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "image/*"
                            }, getString(R.string.pick_from_local_storage)),
                        ArtistDetailActivity.REQUEST_CODE_SELECT_IMAGE)
                true
            }
        }


        menuItem(title = getString(R.string.reset_artist_image)) { //id = R.id.action_reset_artist_image
            icon = getTintedDrawable(R.drawable.ic_close_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                Toast.makeText(context, resources.getString(R.string.updating), Toast.LENGTH_SHORT).show()
                CustomArtistImageStore.instance(context)
                    .resetCustomArtistImage(context, artist.id, artist.name)
                true
            }
        }


        menuItem(title = getString(R.string.action_delete_from_device)) { //id = R.id.action_delete_from_device
            icon = getTintedDrawable(R.drawable.ic_delete_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                fragmentActivity(context) {
                    DeleteSongsDialog.create(ArrayList(artist.songs)).show(it.supportFragmentManager, "ADD_PLAYLIST")
                    true
                }
            }
        }

        menuItem(title = getString(R.string.biography)) { //id = R.id.action_biography
            icon = getTintedDrawable(R.drawable.ic_info_outline_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                loadBiographyCallback(artist)
            }
        }
    }
    true
}

