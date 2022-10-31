/*
 * Copyright (c) 2022 chr_56
 */

@file:JvmName("DisplayableItemRegistry")

package player.phonograph.adapter.display

import player.phonograph.R
import player.phonograph.actions.actionPlay
import player.phonograph.actions.menu.songPopupMenu
import player.phonograph.actions.songClick
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Displayable
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.util.NavigationUtil
import androidx.core.util.Pair
import android.app.Activity
import android.content.Context
import android.view.Menu
import android.view.View
import android.widget.ImageView

/**
 * involve item click
 * @param list      (optional) a list that this Displayable is among
 * @param activity  (optional) for SceneTransitionAnimation
 * @param imageView (optional) item's imagine for SceneTransitionAnimation
 * @return true if action have been processed
 */
fun <T : Displayable> Displayable.tapClick(
    list: List<T>?,
    activity: Activity?,
    imageView: ImageView?,
): Boolean {
    return when (this) {
        is Song   -> {
            val queue = list?.filterIsInstance<Song>()
            if (queue != null && queue.isNotEmpty()) {
                songClick(queue, queue.indexOf(this), true)
            } else {
                this.actionPlay()
            }
            true
        }
        is Album  -> {
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
                        Pair(imageView,
                             imageView.resources.getString(R.string.transition_artist_image))
                    )
                } else {
                    NavigationUtil.goToArtist(activity, this.id)
                }
                true
            } else {
                false
            }
        }
        is Genre  -> {
            if (activity != null) {
                NavigationUtil.goToGenre(activity, this)
                true
            } else {
                false
            }
        }
        else      -> false
    }
}

fun Displayable.hasMenu(): Boolean = this is Song

/**
 * setup three-dot menu for [Song]
 */
fun Displayable.initMenu(
    context: Context,
    menu: Menu,
    showPlay: Boolean = false,
    index: Int = Int.MIN_VALUE,
    transitionView: View? = null,
) =
    if (this is Song) {
        songPopupMenu(context, menu, this, showPlay, index, transitionView)
    } else {
        menu.clear()
    }

/**
 * for fast-scroll recycler-view's bar hint
 */
fun Displayable?.defaultSortOrderReference(): String? =
    when (this) {
        is Song   -> this.title
        is Album  -> this.title
        is Artist -> this.name
        is Genre  -> this.name
        else      -> null
    }
