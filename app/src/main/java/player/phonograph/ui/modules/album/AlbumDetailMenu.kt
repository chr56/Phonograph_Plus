/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.album

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.NavigationUtil
import player.phonograph.ui.actions.actionAddToPlaylist
import player.phonograph.ui.actions.actionDelete
import player.phonograph.ui.actions.actionEnqueue
import player.phonograph.ui.actions.actionPlay
import player.phonograph.ui.actions.actionPlayNext
import player.phonograph.ui.modules.tag.MultiTagBrowserActivity
import player.phonograph.ui.modules.web.LastFmDialog
import player.phonograph.util.fragmentActivity
import player.phonograph.util.theme.getTintedDrawable
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun inflateAlbumDetailMenu(
    menu: Menu,
    context: ComponentActivity,
    item: Album,
    @ColorInt iconColor: Int,
): Boolean =
    with(context) {
        attach(menu) {

            menuItem(title = getString(R.string.action_play)) { //id = R.id.action_shuffle_album
                icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    context.lifecycleScope.launch {
                        item.allSongs(context).actionPlay(ShuffleMode.NONE, 0)
                    }
                    true
                }
            }

            menuItem(title = getString(R.string.action_shuffle_album)) { //id = R.id.action_shuffle_album
                icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    context.lifecycleScope.launch {
                        val songs = item.allSongs(context)
                        songs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(songs.size))
                    }
                    true
                }
            }


            menuItem(title = getString(R.string.action_play_next)) { //id = R.id.action_play_next
                icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    context.lifecycleScope.launch {
                        item.allSongs(context).actionPlayNext()
                    }
                    true
                }
            }


            menuItem(title = getString(R.string.action_add_to_playing_queue)) { //id = R.id.action_add_to_current_playing
                icon = getTintedDrawable(R.drawable.ic_library_add_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    context.lifecycleScope.launch {
                        item.allSongs(context).actionEnqueue()
                    }
                    true
                }
            }

            menuItem(title = getString(R.string.action_add_to_playlist)) { //id = R.id.action_add_to_playlist
                icon = getTintedDrawable(R.drawable.ic_playlist_add_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    context.lifecycleScope.launch {
                        item.allSongs(context).actionAddToPlaylist(context)
                    }
                    true
                }
            }

            menuItem(title = getString(R.string.action_go_to_artist)) { //id = R.id.action_go_to_artist
                icon = getTintedDrawable(R.drawable.ic_person_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    lifecycleScope.launch {
                        NavigationUtil.goToArtist(context, item, null)
                    }
                    true
                }
            }


            menuItem(title = getString(R.string.action_tag_editor)) { //id = R.id.action_tag_editor
                icon = getTintedDrawable(R.drawable.ic_library_music_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    context.lifecycleScope.launch {
                        val songs = item.allSongs(context)
                        MultiTagBrowserActivity.launch(context, ArrayList(songs.map { it.data }))
                    }
                    true
                }
            }


            menuItem(title = getString(R.string.action_delete_from_device)) { //id = R.id.action_delete_from_device
                icon = getTintedDrawable(R.drawable.ic_delete_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    fragmentActivity(context) {
                        context.lifecycleScope.launch {
                            item.allSongs(context).actionDelete(it)
                        }
                        true
                    }
                }
            }

            menuItem(title = getString(R.string.label_wiki)) { //id = R.id.action_wiki
                icon = getTintedDrawable(R.drawable.ic_info_outline_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    if (context is FragmentActivity) {
                        LastFmDialog.from(item).show(context.supportFragmentManager, "LastFmDialog")
                    }
                    true
                }
            }
        }
        true
    }

private suspend fun Album.allSongs(context: Context) =
    withContext(Dispatchers.IO) { Songs.album(context, id) }