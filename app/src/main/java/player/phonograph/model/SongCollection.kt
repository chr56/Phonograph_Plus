/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model

import lib.phonograph.storage.root
import player.phonograph.App
import androidx.annotation.Keep
import androidx.core.content.getSystemService
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.storage.StorageManager

class SongCollection(
    val name: String,
    val songs: List<Song>,
    val detail: String? = null,
) : Parcelable, Displayable {


    override fun getItemID(): Long = hashCode().toLong()
    override fun getDisplayTitle(context: Context) = name
    override fun getDescription(context: Context) =
        "${songCountString(context, songs.size)} ...${stripStorageVolume(detail.orEmpty())}"


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SongCollection) return false

        if (name != other.name) return false
        if (songs != other.songs) return false
        if (detail != other.detail) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode() * 31 + songs.hashCode()

    override fun toString(): String = "SongCollection(name='$name', songs=$songs)"

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.createTypedArrayList(Song.CREATOR)!!,
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeTypedList(songs)
        parcel.writeString(detail)
    }

    override fun describeContents(): Int = 0

    companion object {
        @Keep
        @JvmField
        val CREATOR = object : Parcelable.Creator<SongCollection> {
            override fun createFromParcel(parcel: Parcel): SongCollection {
                return SongCollection(parcel)
            }

            override fun newArray(size: Int): Array<SongCollection?> {
                return arrayOfNulls(size)
            }
        }

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