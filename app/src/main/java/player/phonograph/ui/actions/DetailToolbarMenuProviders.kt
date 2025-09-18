/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.actions

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.coil.CustomArtistImageStore
import player.phonograph.mechanism.playlist.PlaylistSongsActions
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.modules.tag.MultiTagBrowserActivity
import player.phonograph.ui.modules.web.LastFmDialog
import player.phonograph.util.NavigationUtil
import player.phonograph.util.component.IGetContentRequester
import player.phonograph.util.fragmentActivity
import player.phonograph.util.theme.getTintedDrawable
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DetailToolbarMenuProviders {


    interface ToolbarMenuProvider<I> {
        /**
         * inflate [menu] of this [item]
         */
        fun inflateMenu(
            menu: Menu,
            context: ComponentActivity,
            item: I,
            @ColorInt iconColor: Int,
        ): Boolean
    }

    object AlbumToolbarMenuProvider : ToolbarMenuProvider<Album> {
        override fun inflateMenu(
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
                                if (item.artistName != null) {
                                    NavigationUtil.goToArtist(context, item.artistName, null)
                                } else {
                                    NavigationUtil.goToArtist(context, item.artistId, null)
                                }
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
    }

    object ArtistToolbarMenuProvider : ToolbarMenuProvider<Artist> {
        override fun inflateMenu(
            menu: Menu,
            context: ComponentActivity,
            item: Artist,
            @ColorInt iconColor: Int,
        ): Boolean =
            with(context) {
                attach(menu) {

                    menuItem(title = getString(R.string.action_play)) { //id = R.id.action_shuffle_artist
                        icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                        onClick {
                            context.lifecycleScope.launch {
                                item.allSongs(context).actionPlay(ShuffleMode.NONE, 0)
                            }
                            true
                        }
                    }

                    menuItem(title = getString(R.string.action_shuffle_artist)) { //id = R.id.action_shuffle_artist
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

                    menuItem(title = getString(R.string.action_set_artist_image)) { //id = R.id.action_set_artist_image
                        icon = getTintedDrawable(R.drawable.ic_person_white_24dp, iconColor)
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                        onClick {
                            if (context is IGetContentRequester) {
                                context.getContentDelegate.launch("image/*") {
                                    if (it != null)
                                        CustomArtistImageStore.instance(context)
                                            .setCustomArtistImage(context, item.id, item.name, it)
                                }
                                true
                            } else {
                                false
                            }
                        }
                    }


                    menuItem(title = getString(R.string.action_reset_artist_image)) { //id = R.id.action_reset_artist_image
                        icon = getTintedDrawable(R.drawable.ic_close_white_24dp, iconColor)
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                        onClick {
                            Toast.makeText(context, resources.getString(R.string.state_updating), Toast.LENGTH_SHORT)
                                .show()
                            CustomArtistImageStore.instance(context)
                                .resetCustomArtistImage(context, item.id, item.name)
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
                            context.lifecycleScope.launch {
                                item.allSongs(context).actionDelete(context)
                            }
                            true
                        }
                    }


                    menuItem(title = getString(R.string.label_biography)) { //id = R.id.action_biography
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

        private suspend fun Artist.allSongs(context: Context) =
            withContext(Dispatchers.IO) { Songs.artist(context, id) }
    }

    object GenreEntityToolbarMenuProvider : ToolbarMenuProvider<Genre> {
        override fun inflateMenu(
            menu: Menu,
            context: ComponentActivity,
            item: Genre,
            @ColorInt iconColor: Int,
        ): Boolean =
            with(context) {
                attach(menu) {
                    menuItem(getString(R.string.action_play)) {
                        icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                        onClick {
                            context.lifecycleScope.launch {
                                val allSongs = item.allSongs(context)
                                allSongs.actionPlay(ShuffleMode.NONE, 0)
                            }
                            true
                        }
                    }
                    menuItem(getString(R.string.action_shuffle_playlist)) {
                        icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                        onClick {
                            context.lifecycleScope.launch {
                                val allSongs = item.allSongs(context)
                                allSongs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(allSongs.size))
                            }
                            true
                        }
                    }
                    menuItem(getString(R.string.action_play_next)) { //id = R.id.action_play_next
                        icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                        onClick {
                            context.lifecycleScope.launch {
                                val allSongs = item.allSongs(context)
                                allSongs.actionPlayNext()
                            }
                            true
                        }
                    }
                }
                true
            }

        private suspend fun Genre.allSongs(context: Context) =
            withContext(Dispatchers.IO) { Songs.genres(context, id) }
    }

    object PlaylistEntityToolbarMenuProvider : ToolbarMenuProvider<Playlist> {
        override fun inflateMenu(
            menu: Menu,
            context: ComponentActivity,
            item: Playlist,
            iconColor: Int,
        ): Boolean = with(context) {
            attach(menu) {
                menuItem {
                    title = getString(R.string.action_play)
                    icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                    onClick {
                        context.lifecycleScope.launch {
                            item.actionPlay(context)
                        }
                        true
                    }
                }
                menuItem {
                    title = getString(R.string.action_shuffle_playlist)
                    icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                    onClick {
                        context.lifecycleScope.launch {
                            item.actionShuffleAndPlay(context)
                        }
                        true
                    }
                }
                menuItem {
                    title = getString(R.string.action_play_next)
                    icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        context.lifecycleScope.launch {
                            item.actionPlayNext(context)
                        }
                        true
                    }
                }
                menuItem {
                    title = getString(R.string.action_add_to_playing_queue)
                    icon = getTintedDrawable(R.drawable.ic_library_add_white_24dp, iconColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        context.lifecycleScope.launch {
                            item.actionAddToCurrentQueue(context)
                        }
                        true
                    }
                }
                menuItem {
                    title = getString(R.string.action_add_to_playlist)
                    icon = getTintedDrawable(R.drawable.ic_playlist_add_white_24dp, iconColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        fragmentActivity(context) {
                            context.lifecycleScope.launch {
                                item.actionAddToPlaylist(it)
                            }
                            true
                        }
                    }
                }
                if (!item.isVirtual()) {
                    menuItem {
                        title = getString(R.string.action_rename)
                        icon = getTintedDrawable(R.drawable.ic_edit_white_24dp, iconColor)
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                        onClick {
                            fragmentActivity(context) {
                                item.actionRenamePlaylist(it)
                                true
                            }
                        }
                    }
                }
                menuItem {
                    title = getString(
                        if (!item.isVirtual()) R.string.action_delete else R.string.action_clear
                    )
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        fragmentActivity(context) {
                            item.actionDeletePlaylist(it)
                            true
                        }
                    }
                }
                menuItem(title = getString(R.string.action_tag_editor)) { //id = R.id.action_tag_editor
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        context.lifecycleScope.launch(Dispatchers.IO) {
                            val paths = PlaylistSongsActions.reader(item).allSongs(context).map { it.data }
                            MultiTagBrowserActivity.launch(context, ArrayList(paths))
                        }
                        true
                    }
                }
                menuItem {
                    title = getString(R.string.action_save_playlist)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        fragmentActivity(context) {
                            it.lifecycleScope.launch {
                                item.actionSavePlaylist(it)
                            }
                            true
                        }
                    }
                }
            }
            true
        }
    }

}