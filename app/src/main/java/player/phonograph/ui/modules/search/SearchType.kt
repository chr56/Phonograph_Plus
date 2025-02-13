/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.search

import player.phonograph.R
import androidx.annotation.StringRes

enum class SearchType(@param:StringRes val nameRes: Int) {
    SONGS(R.string.label_songs),
    ALBUMS(R.string.label_albums),
    ARTISTS(R.string.label_artists),
    PLAYLISTS(R.string.label_playlists),
    GENRES(R.string.label_genres),
    QUEUE(R.string.label_playing_queue);
}