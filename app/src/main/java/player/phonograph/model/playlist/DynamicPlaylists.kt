/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import android.content.res.Resources

object DynamicPlaylists {

    fun favorites(resources: Resources): Playlist =
        Playlist(
            resources.getString(R.string.favorites),
            VirtualPlaylistLocation(PLAYLIST_TYPE_FAVORITE),
            iconRes = R.drawable.ic_favorite_white_24dp
        )

    fun history(resources: Resources): Playlist =
        Playlist(
            resources.getString(R.string.history),
            VirtualPlaylistLocation(PLAYLIST_TYPE_HISTORY),
            iconRes = R.drawable.ic_access_time_white_24dp
        )

    fun lastAdded(resources: Resources): Playlist =
        Playlist(
            resources.getString(R.string.last_added),
            VirtualPlaylistLocation(PLAYLIST_TYPE_LAST_ADDED),
            iconRes = R.drawable.ic_library_add_white_24dp
        )

    fun myTopTrack(resources: Resources): Playlist =
        Playlist(
            resources.getString(R.string.my_top_tracks),
            VirtualPlaylistLocation(PLAYLIST_TYPE_MY_TOP_TRACK),
            iconRes = R.drawable.ic_trending_up_white_24dp
        )

    fun random(resources: Resources): Playlist =
        Playlist(
            resources.getString(R.string.action_shuffle_all),
            VirtualPlaylistLocation(PLAYLIST_TYPE_RANDOM),
            iconRes = R.drawable.ic_shuffle_white_24dp
        )
}