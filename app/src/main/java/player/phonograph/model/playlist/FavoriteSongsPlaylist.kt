/*
 *  Copyright (c) 2022~2023 chr_56
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
