/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.model.playlist

import android.os.Parcel
import player.phonograph.PlaylistType


abstract class SmartPlaylist : Playlist, GeneratedPlaylist {
    constructor() : super()
    constructor(id: Long, name: String?) : super(id, name)
    override val type: Int
        get() = PlaylistType.ABS_SMART
    constructor(parcel: Parcel) : super(parcel)
}
