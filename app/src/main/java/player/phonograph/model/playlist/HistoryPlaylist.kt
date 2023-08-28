/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import android.content.Context
import android.os.Parcel

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