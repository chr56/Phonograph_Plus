/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.config

import player.phonograph.model.IMAGE_SOURCE_EXTERNAL_FILE
import player.phonograph.model.IMAGE_SOURCE_J_AUDIO_TAGGER
import player.phonograph.model.IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER
import player.phonograph.model.IMAGE_SOURCE_MEDIA_STORE
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

    companion object {
        @Suppress("PropertyName")
        val DEFAULT: ImageSourceConfig
            get() = ImageSourceConfig(
                listOf(
                    Item(IMAGE_SOURCE_MEDIA_STORE, true),
                    Item(IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER, true),
                    Item(IMAGE_SOURCE_J_AUDIO_TAGGER, true),
                    Item(IMAGE_SOURCE_EXTERNAL_FILE, true),
                ), VERSION
            )
        const val VERSION = 1
    }
}

