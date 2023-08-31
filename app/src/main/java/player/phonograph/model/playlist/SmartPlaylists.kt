/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.model.playlist

import player.phonograph.R
import android.content.Context
import android.os.Parcel

abstract class FavoriteSongsPlaylist : SmartPlaylist, EditablePlaylist {

    constructor(context: Context) : super(
        "FavoritePlaylist".hashCode() * 10000019L,
        context.getString(R.string.favorites)
    )

    override val type: Int
        get() = PlaylistType.FAVORITE

    override var iconRes: Int = R.drawable.ic_favorite_border_white_24dp

    override fun toString(): String = "FavoritePlaylist"

    constructor(parcel: Parcel) : super(parcel)
}

abstract class ShuffleAllPlaylist : SmartPlaylist {
    constructor(context: Context) : super(
        "shuffle_all".hashCode() * 10000019L,
        context.getString(R.string.action_shuffle_all)
    )

    override val type: Int
        get() = PlaylistType.RANDOM

    override val iconRes: Int = R.drawable.ic_shuffle_white_24dp

    override fun toString(): String = "ShuffleAllPlaylist"

    constructor(parcel: Parcel) : super(parcel)
}

abstract class LastAddedPlaylist : SmartPlaylist {
    constructor(context: Context) : super(
        "last_added".hashCode() * 31L + R.drawable.ic_library_add_white_24dp,
        context.getString(R.string.last_added)
    )

    override val type: Int
        get() = PlaylistType.LAST_ADDED

    override var iconRes: Int = R.drawable.ic_library_add_white_24dp

    override fun toString(): String = "LastAddedPlaylist"

    constructor(parcel: Parcel) : super(parcel)

}

abstract class HistoryPlaylist : SmartPlaylist, ResettablePlaylist {

    constructor(context: Context) : super(
        "recently_played".hashCode() * 10000019L,
        context.getString(R.string.history)
    )

    override val type: Int
        get() = PlaylistType.HISTORY

    override var iconRes: Int = R.drawable.ic_access_time_white_24dp

    override fun toString(): String = "HistoryPlaylist"

    constructor(parcel: Parcel) : super(parcel)
}

abstract class MyTopTracksPlaylist : SmartPlaylist, ResettablePlaylist {
    constructor(context: Context) : super(
        "MyTopTracksPlaylist".hashCode() * 10000019L,
        context.getString(R.string.my_top_tracks)
    )

    override val type: Int
        get() = PlaylistType.MY_TOP_TRACK

    override var iconRes: Int = R.drawable.ic_trending_up_white_24dp

    override fun toString(): String = "MyTopTracksPlaylist"

    constructor(parcel: Parcel) : super(parcel)

}