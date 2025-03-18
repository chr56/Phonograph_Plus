/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.version

import androidx.annotation.Keep
import android.content.res.Resources
import android.os.Parcelable
import android.text.Html
import android.text.Spanned
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName

@Keep
@Parcelize
@kotlinx.serialization.Serializable
data class Version(
    val channel: String = "na",
    val link: List<Link> = emptyList(),
    val releaseNote: ReleaseNote = ReleaseNote(),
    val versionName: String = "unknown",
    val versionCode: Int = -1,
    val date: Long = 0,
) : Parcelable {

    @Keep
    @Parcelize
    @kotlinx.serialization.Serializable
    data class Link(
        val name: String = "",
        val uri: String = "",
    ) : Parcelable

    @Keep
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
