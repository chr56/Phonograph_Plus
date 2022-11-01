/*
 * Copyright (c) 2022 chr_56
 */

@file:JvmName("DisplayableItemRegistry")

package player.phonograph.adapter.display

import player.phonograph.actions.menu.songPopupMenu
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Displayable
import player.phonograph.model.Genre
import player.phonograph.model.Song
import android.content.Context
import android.view.Menu
import android.view.View

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
