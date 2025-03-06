/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.coil

import androidx.annotation.Keep
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Parcelize
@Serializable
data class ImageSourceConfig(
    @SerialName("sources") val sources: List<Item>,
    @SerialName("version") val version: Int = 0
) : Parcelable {
    @Keep
    @Parcelize
    @Serializable
    data class Item(
        @SerialName("key") val key: String,
        @SerialName("enabled") val enabled: Boolean
    ) : Parcelable {
        val imageSource: ImageSource
            get() = ImageSource.fromKey(key)
    }

    companion object {
        fun from(source: List<Item>): ImageSourceConfig {
            return ImageSourceConfig(source, VERSION)
        }

        val DEFAULT: ImageSourceConfig
            get() = ImageSourceConfig(
                listOf(
                    Item(ImageSource.IMAGE_SOURCE_MEDIA_STORE, true),
                    Item(ImageSource.IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER, true),
                    Item(ImageSource.IMAGE_SOURCE_J_AUDIO_TAGGER, true),
                    Item(ImageSource.IMAGE_SOURCE_EXTERNAL_FILE, true),
                ), VERSION
            )

        const val VERSION = 1
    }
}

