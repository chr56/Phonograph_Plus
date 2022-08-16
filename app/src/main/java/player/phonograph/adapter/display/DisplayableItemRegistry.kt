/*
 * Copyright (c) 2022 chr_56
 */

@file:JvmName("DisplayableItemRegistry")

package player.phonograph.adapter.display

import android.app.Activity
import android.widget.ImageView
import androidx.annotation.MenuRes
import androidx.core.util.Pair
import androidx.fragment.app.FragmentActivity
import player.phonograph.R
import player.phonograph.mediastore.AlbumLoader.allSongs
import player.phonograph.mediastore.ArtistLoader.allArtistSongs
import player.phonograph.mediastore.GenreLoader.allGenreSongs
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Displayable
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.util.NavigationUtil
import player.phonograph.util.menu.onMultiSongMenuItemClick
import player.phonograph.util.menu.onSongMenuItemClick

/**
 * involve item click
 * @param list      (optional) a list that this Displayable is among
 * @param activity  (optional) for SceneTransitionAnimation
 * @param imageView (optional) item's imagine for SceneTransitionAnimation
 * @return true if action have been processed
 */
fun Displayable.clickHandler(list: List<Displayable>?, activity: Activity?, imageView: ImageView?): Boolean {
    return when (this) {
        is Song -> {
            val contextQueue = list?.filterIsInstance<Song>()
            if (!Setting.instance.keepPlayingQueueIntact && contextQueue != null) {
                MusicPlayerRemote.openQueue(contextQueue, contextQueue.indexOf(this), true)
            } else {
                MusicPlayerRemote.playNow(this)
            }
            true
        }
        is Album -> {
            if (activity != null) {
                if (imageView != null) {
                    NavigationUtil.goToAlbum(
                        activity,
                        this.id,
                        Pair(
                            imageView,
                            imageView.resources.getString(R.string.transition_album_art)
                        )
                    )
                } else {
                    NavigationUtil.goToAlbum(
                        activity,
                        this.id
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
                        this.id,
                        Pair(
                            imageView,
                            imageView.resources.getString(R.string.transition_artist_image)
                        )
                    )
                } else {
                    NavigationUtil.goToArtist(
                        activity,
                        this.id
                    )
                }
                true
            } else {
                false
            }
        }
        is Genre -> {
            if (activity != null) {
                NavigationUtil.goToGenre(activity, this)
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
fun Displayable.menuRes(): Int = when (this) {
    is Song -> R.menu.menu_item_song_short
    else -> 0
}

/**
 * invoke menu item click
 * @param actionId ItemId in menu as well as `Unique Action ID`
 * @param activity as [android.content.Context]
 * @return true if action have been processed
 */
fun Displayable.menuClick(actionId: Int, activity: FragmentActivity): Boolean {
    return when (this) {
        is Song ->
            onSongMenuItemClick(
                activity,
                this,
                actionId
            )
        else -> false
    }
}

/**
 * invoke menu item click
 * @param actionId ItemId in menu as well as `Unique Action ID`
 * @param activity as [android.content.Context]
 * @return true if action have been processed
 */
fun List<Displayable>.multiMenuClick(actionId: Int, activity: FragmentActivity): Boolean {
    val songs = when (val sample = this.getOrNull(0)) {
        is Song -> this.filterIsInstance<Song>()
        is Album -> this.filterIsInstance<Album>().allSongs()
        is Artist -> this.filterIsInstance<Artist>().allArtistSongs()
        is Genre -> this.filterIsInstance<Genre>().allGenreSongs()
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
fun Displayable?.defaultSortOrderReference(): String? =
    when (this) {
        is Song -> this.title
        is Album -> this.title
        is Artist -> this.name
        is Genre -> this.name
        else -> null
    }
