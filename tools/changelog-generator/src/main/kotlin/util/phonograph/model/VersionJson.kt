/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class VersionJson(
    @SerialName("versions")
    val versions: List<Item>,
) {

    @Serializable
    class Item(
        @SerialName(CHANNEL) val channel: String,
        @SerialName(VERSION_NAME) val versionName: String,
        @SerialName(VERSION_CODE) val versionCode: Int,
        @SerialName(DATE) val date: Long,
        @SerialName(LINK) val link: List<Link>,
        @SerialName(RELEASE_NOTE) val releaseNote: ReleaseNote,
    ) {
        @Serializable
        class Link(
            @SerialName(LINK_NAME) val name: String,
            @SerialName(LINK_URI) val url: String,
        )
        @Serializable
        class ReleaseNote(
            @SerialName(ZH_CN) val zh: String,
            @SerialName(EN) val en: String,
        )
    }
}


private const val CHANNEL = "channel"
private const val VERSION_NAME = "versionName"
private const val VERSION_CODE = "versionCode"
private const val DATE = "date"
private const val LINK = "link"
private const val LINK_NAME = "name"
private const val LINK_URI = "uri"
private const val RELEASE_NOTE = "releaseNote"
private const val ZH_CN = "zh-cn"
private const val EN = "en"
