/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.auxiliary

import de.psdev.licensesdialog.LicenseResolver
import de.psdev.licensesdialog.model.Notice
import de.psdev.licensesdialog.model.Notices
import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
private class NoticeText(
    @SerialName("name") var name: String? = null,
    @SerialName("url") var url: String? = null,
    @SerialName("copyright") var copyright: String? = null,
    @SerialName("license") var license: String? = null,
) {
    fun toNotice(): Notice = Notice(name, url, copyright, LicenseResolver.read(license))
}

@Serializable
private class NoticesText(
    @SerialName("notices") val notices: List<NoticeText>,
) {
    fun toNotices(): Notices = Notices().also { n -> notices.forEach { n.addNotice(it.toNotice()) } }
}

object LicenseNoticesProcessor {

    fun readNotices(context: Context, path: String): Notices {
        val inputStream = context.assets.open(path)
        val notices = inputStream.reader().use {
            readFrom(it.readText())
        }
        return notices
    }

    private val parser = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private fun readFrom(data: String): Notices {
        val model: NoticesText = parser.decodeFromString(NoticesText.serializer(), data)
        return model.toNotices()
    }
}

