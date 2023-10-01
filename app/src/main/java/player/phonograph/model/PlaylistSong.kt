package player.phonograph.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class PlaylistSong(
    val song: Song,
    val playlistId: Long,
    val idInPlayList: Long,
) : Parcelable, Displayable by song