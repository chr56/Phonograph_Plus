/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import android.content.Context
import android.os.Parcel

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