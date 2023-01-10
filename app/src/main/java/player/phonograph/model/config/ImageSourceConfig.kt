/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.config

import player.phonograph.model.ImageSource
import androidx.annotation.Keep
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@kotlinx.serialization.Serializable
data class ImageSourceConfig(
    val list: List<Item>,
    val version: Int = 0
) : Parcelable {
    @Keep
    @Parcelize
    @kotlinx.serialization.Serializable
    data class Item(
        val name: String,
        val enabled: Boolean
    ) : Parcelable {
        val imageSource: ImageSource
            get() = ImageSource.fromKey(name)
    }
}