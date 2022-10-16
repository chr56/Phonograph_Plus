/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.version

import android.content.res.Resources
import android.os.Parcelable
import android.text.Html
import android.text.Spanned
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import player.phonograph.BuildConfig
import java.util.*


@kotlinx.serialization.Serializable
@Parcelize
class VersionCatalog(
    val versions: List<Version> = emptyList(),
) : Parcelable {
    val channelVersions: List<Version>
        get() = versions.filter { version -> version.channel == currentChannel }

    fun <R : Comparable<R>> currentLatestChannelVersionBy(selector: (Version) -> R): Version =
        with(channelVersions) {
            maxByOrNull(selector) ?: Version()
        }
}

@Parcelize
@kotlinx.serialization.Serializable
data class Version(
    val channel: String = currentChannel,
    val link: List<Link> = emptyList(),
    val releaseNote: ReleaseNote = ReleaseNote(),
    val versionName: String = "unknown",
    val versionCode: Int = -1,
    val date: Long = 0,
) : Parcelable {
    @Parcelize
    @kotlinx.serialization.Serializable
    data class Link(
        val name: String = "",
        val uri: String = "",
    ) : Parcelable

    @Parcelize
    @kotlinx.serialization.Serializable
    data class ReleaseNote(
        val en: String = "",
        @SerialName("zh-cn")
        val zh_cn: String = "",
    ) : Parcelable {
        fun parsed(resources: Resources): Spanned {
            val lang = resources.configuration.locales.get(0)
            val source = if (lang.language.lowercase() == "zh") zh_cn else en
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        }
    }
}


val currentChannel: String by lazy {
    val flavor = BuildConfig.FLAVOR.lowercase()
    when {
        flavor.contains("preview") -> "preview"
        else -> "stable"
    }
}