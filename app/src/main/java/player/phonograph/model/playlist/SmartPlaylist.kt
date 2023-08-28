/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.playlist

import org.koin.core.context.GlobalContext
import android.os.Parcel

abstract class SmartPlaylist : Playlist, GeneratedPlaylist {
    constructor() : super()
    constructor(id: Long, name: String?) : super(id, name)
    override val type: Int get() = PlaylistType.ABS_SMART
    constructor(parcel: Parcel) : super(parcel)

    companion object {
        val favoriteSongsPlaylist: FavoriteSongsPlaylist get() = GlobalContext.get().get()
        val historyPlaylist: HistoryPlaylist get() = GlobalContext.get().get()
        val lastAddedPlaylist: LastAddedPlaylist get() = GlobalContext.get().get()
        val myTopTracksPlaylist: MyTopTracksPlaylist get() = GlobalContext.get().get()
        val shuffleAllPlaylist: ShuffleAllPlaylist get() = GlobalContext.get().get()
    }
}