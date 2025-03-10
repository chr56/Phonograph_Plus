package player.phonograph.model

import player.phonograph.util.text.infoString
import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Genre(
    @JvmField val id: Long,
    @JvmField val name: String?,
    @JvmField val songCount: Int,
) : Parcelable, Displayable {


    override fun getItemID(): Long = id

    override fun getDisplayTitle(context: Context): CharSequence = name ?: "UNKNOWN GENRE $id"

    override fun getDescription(context: Context): CharSequence = infoString(context)

    override fun defaultSortOrderReference(): String? = name
}
