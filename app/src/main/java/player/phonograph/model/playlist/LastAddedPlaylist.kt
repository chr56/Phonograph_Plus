/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import android.content.Context
import android.os.Parcel

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