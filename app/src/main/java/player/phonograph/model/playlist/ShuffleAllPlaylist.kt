/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import android.content.Context
import android.os.Parcel

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
