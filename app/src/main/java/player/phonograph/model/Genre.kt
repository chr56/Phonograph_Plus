package player.phonograph.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Genre(
    @JvmField val id: Long,
    @JvmField val name: String?,
    @JvmField val songCount: Int,
) : Parcelable
