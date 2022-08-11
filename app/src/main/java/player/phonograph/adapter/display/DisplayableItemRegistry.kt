/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.adapter.display

import android.app.Activity
import android.widget.ImageView
import androidx.annotation.MenuRes
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import player.phonograph.R
import player.phonograph.interfaces.Displayable
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.util.MusicUtil
import player.phonograph.util.NavigationUtil
import player.phonograph.util.menu.onMultiSongMenuItemClick
import player.phonograph.util.menu.onSongMenuItemClick

object DisplayableItemRegistry {

    /**
     * involve item click
     * @param item      action's target
     * @param list      (optional) a list that [item] is among
     * @param activity  (optional) for SceneTransitionAnimation
     * @param imageView (optional) item's imagine for SceneTransitionAnimation
     * @return true if action have been processed
     */
    fun clickHandler(
        item: Displayable,
        list: List<Displayable>?,
        activity: Activity?,
        imageView: ImageView?
    ): Boolean {
        return when (item) {
            is Song -> {
                val queue = list?.filterIsInstance<Song>()
                if (queue != null) {
                    if (Setting.instance.keepPlayingQueueIntact) {
                        MusicPlayerRemote.playNow(queue[queue.indexOf(item)])
                    } else {
                        MusicPlayerRemote.openQueue(queue, queue.indexOf(item), true)
                    }
                } else {
                    MusicPlayerRemote.playNext(item)
                }
                true
            }
            is Album -> {
                if (activity != null) {
                    if (imageView != null) {
                        NavigationUtil.goToAlbum(
                            activity,
                            item.id,
                            Pair(
                                imageView,
                                imageView.resources.getString(R.string.transition_album_art)
                            )
                        )
                    } else {
                        NavigationUtil.goToAlbum(
                            activity,
                            item.id
                        )
                    }
                    true
                } else {
                    false
                }
            }
            is Artist -> {
                if (activity != null) {
                    if (imageView != null) {
                        NavigationUtil.goToArtist(
                            activity,
                            item.id,
                            Pair(
                                imageView,
                                imageView.resources.getString(R.string.transition_artist_image)
                            )
                        )
                    } else {
                        NavigationUtil.goToArtist(
                            activity,
                            item.id
                        )
                    }
                    true
                } else {
                    false
                }
            }
            is Genre -> {
                if (activity != null) {
                    NavigationUtil.goToGenre(activity, item)
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    /**
     * @return correspond Menu Res ID for item
     */
    @MenuRes
    fun menuRes(item: Displayable): Int = when (item) {
        is Song -> R.menu.menu_item_song_short
        else -> 0
    }

    /**
     * invoke menu item click
     * @param item     action's target
     * @param actionId ItemId in menu as well as `Unique Action ID`
     * @param activity as [android.content.Context]
     * @return true if action have been processed
     */
    fun menuClick(item: Displayable, actionId: Int, activity: FragmentActivity): Boolean {
        return when (item) {
            is Song ->
                onSongMenuItemClick(
                    activity,
                    item,
                    actionId
                )
            else -> false
        }
    }

    /**
     * invoke menu item click
     * @param list     action's target
     * @param actionId ItemId in menu as well as `Unique Action ID`
     * @param activity as [android.content.Context]
     * @return true if action have been processed
     */
    fun multiMenuClick(list: List<Displayable>, actionId: Int, activity: FragmentActivity): Boolean {
        val songs = when (val sample = list.getOrNull(0)) {
            is Song -> list.filterIsInstance<Song>()
            is Album -> MusicUtil.getAlbumSongList(list.filterIsInstance<Album>())
            is Artist -> MusicUtil.getArtistSongList(list.filterIsInstance<Artist>())
            is Genre -> MusicUtil.getGenreSongList(list.filterIsInstance<Genre>())
            else -> emptyList()
        }
        return if (songs.isNotEmpty()) {
            onMultiSongMenuItemClick(activity, songs, actionId)
        } else {
            false
        }
    }

    /**
     * for fast-scroll recycler-view's bar hint
     */
    fun defaultSortOrderReference(item: Displayable?): String? =
        when (item) {
            is Song -> item.title
            is Album -> item.title
            is Artist -> item.name
            is Genre -> item.name
            else -> null
        }
}
