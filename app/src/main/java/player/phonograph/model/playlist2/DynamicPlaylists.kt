/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.model.playlist2

import player.phonograph.R

object DynamicPlaylists {

    fun favorite(): Playlist =
        Playlist(
            (-PLAYLIST_TYPE_FAVORITE).toLong(),
            "Favorite",
            VirtualPlaylistLocation(PLAYLIST_TYPE_FAVORITE),
            iconRes = R.drawable.ic_favorite_white_24dp
        )

    fun history(): Playlist =
        Playlist(
            (-PLAYLIST_TYPE_HISTORY).toLong(),
            "History",
            VirtualPlaylistLocation(PLAYLIST_TYPE_HISTORY),
            iconRes = R.drawable.ic_access_time_white_24dp
        )

    fun lastAdded(): Playlist =
        Playlist(
            (-PLAYLIST_TYPE_LAST_ADDED).toLong(),
            "LastAdded",
            VirtualPlaylistLocation(PLAYLIST_TYPE_LAST_ADDED),
            iconRes = R.drawable.ic_library_add_white_24dp
        )

    fun myTopTrack(): Playlist =
        Playlist(
            (-PLAYLIST_TYPE_MY_TOP_TRACK).toLong(),
            "MyTopTrack",
            VirtualPlaylistLocation(PLAYLIST_TYPE_MY_TOP_TRACK),
            iconRes = R.drawable.ic_trending_up_white_24dp
        )

    fun random(): Playlist =
        Playlist(
            (-PLAYLIST_TYPE_RANDOM).toLong(),
            "Random",
            VirtualPlaylistLocation(PLAYLIST_TYPE_RANDOM),
            iconRes = R.drawable.ic_shuffle_white_24dp
        )
}