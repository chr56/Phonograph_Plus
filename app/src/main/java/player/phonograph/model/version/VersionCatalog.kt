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
    val versions: List<Version>,
) : Parcelable {
    val channelVersions: List<Version>
        get() = versions.filter { version -> version.channel == currentChannel }

    fun <R : Comparable<R>> currentLatestChannelVersionBy(selector: (Version) -> R): Version =
        with(channelVersions) {
            maxByOrNull(selector) ?: firstOrNull() ?: Version.EMPTY_VERSION
        }
}

@Parcelize
@kotlinx.serialization.Serializable
data class Version(
    val channel: String,
    val link: List<Link>,
    val releaseNote: ReleaseNote,
    val versionName: String,
    val versionCode: Int,
    val date: Long,
) : Parcelable {
    @Parcelize
    @kotlinx.serialization.Serializable
    data class Link(
        val name: String,
        val uri: String,
    ) : Parcelable

    @Parcelize
    @kotlinx.serialization.Serializable
    data class ReleaseNote(
        val en: String,
        @SerialName("zh-cn")
        val zh_cn: String,
    ) : Parcelable {
        fun parsed(resources: Resources): Spanned {
            val lang = resources.configuration.locales.get(0)
            val zhs = Locale.SIMPLIFIED_CHINESE
            val source = if (lang.equals(zhs)) zh_cn else en
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        }
    }

    companion object {
        internal val EMPTY_VERSION
            get() = Version(
                currentChannel,
                emptyList(),
                ReleaseNote("Error", "Error"),
                "Unknown",
                0,
                0
            )
    }
}


val currentChannel: String by lazy {
    val flavor = BuildConfig.FLAVOR.lowercase()
    when {
        flavor.contains("preview") -> "preview"
        else -> "stable"
    }
}