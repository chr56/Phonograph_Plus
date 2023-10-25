/*
 *  Copyright (c) 2022~2023 chr_56
 */

@file:JvmName("DisplayableItemRegistry")

package player.phonograph.ui.adapter

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Displayable
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist

fun Displayable.hasMenu(): Boolean = this is Song || this is Playlist

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
