/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model

import lib.storage.root
import player.phonograph.App
import androidx.core.content.getSystemService
import android.content.Context
import android.os.Parcelable
import android.os.storage.StorageManager
import kotlinx.parcelize.Parcelize

@Parcelize
data class SongCollection(
    val name: String,
    val songs: List<Song>,
    val detail: String? = null,
) : Parcelable, Displayable {


    override fun getItemID(): Long = hashCode().toLong()
    override fun getDisplayTitle(context: Context) = name
    override fun getDescription(context: Context) =
        "${songCountString(context, songs.size)} ...${stripStorageVolume(detail.orEmpty())}"

    override fun getSecondaryText(context: Context): CharSequence = stripStorageVolume(detail.orEmpty())
    override fun getTertiaryText(context: Context): CharSequence = songCountString(context, songs.size)

    companion object {

        private fun stripStorageVolume(str: String): String {
            return str.removePrefix(internalStorageRootPath).removePrefix("/storage")
        }

        private val internalStorageRootPath: String by lazy {
            val storageManager = App.instance.getSystemService<StorageManager>()!!
            val storageVolume = storageManager.primaryStorageVolume
            storageVolume.root()?.absolutePath ?: ""
        }
    }

}