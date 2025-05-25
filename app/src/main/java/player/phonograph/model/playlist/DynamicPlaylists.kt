/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import android.content.res.Resources

object DynamicPlaylists {

    fun favorites(resources: Resources): Playlist =
        Playlist(
            resources.getString(R.string.playlist_favorites),
            VirtualPlaylistLocation.Favorite,
            iconRes = R.drawable.ic_favorite_white_24dp
        )

    fun history(resources: Resources): Playlist =
        Playlist(
            resources.getString(R.string.playlist_history),
            VirtualPlaylistLocation.History,
            iconRes = R.drawable.ic_access_time_white_24dp
        )

    fun lastAdded(resources: Resources): Playlist =
        Playlist(
            resources.getString(R.string.playlist_last_added),
            VirtualPlaylistLocation.LastAdded,
            iconRes = R.drawable.ic_library_add_white_24dp
        )

    fun myTopTrack(resources: Resources): Playlist =
        Playlist(
            resources.getString(R.string.playlist_my_top_tracks),
            VirtualPlaylistLocation.MyTopTrack,
            iconRes = R.drawable.ic_trending_up_white_24dp
        )

    fun random(resources: Resources): Playlist =
        Playlist(
            resources.getString(R.string.action_shuffle_all),
            VirtualPlaylistLocation.Random,
            iconRes = R.drawable.ic_shuffle_white_24dp
        )
}