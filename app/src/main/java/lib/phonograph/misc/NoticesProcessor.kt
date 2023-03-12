/*
 *  Copyright (c) 2023 chr_56
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 */

package lib.phonograph.misc

import de.psdev.licensesdialog.LicenseResolver
import de.psdev.licensesdialog.model.Notice
import de.psdev.licensesdialog.model.Notices
import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
class NoticeText(
    @SerialName("name") var name: String? = null,
    @SerialName("url") var url: String? = null,
    @SerialName("copyright") var copyright: String? = null,
    @SerialName("license") var license: String? = null,
) {
    fun toNotice(): Notice = Notice(name, url, copyright, LicenseResolver.read(license))
}

@Serializable
class NoticesText(
    @SerialName("notices") val notices: List<NoticeText>,
) {
    fun toNotices(): Notices = Notices().also { n -> notices.forEach { n.addNotice(it.toNotice()) } }
}

object NoticesProcessor {
    private val parser = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private fun readFrom(data: String): Notices {
        val model: NoticesText = parser.decodeFromString(NoticesText.serializer(), data)
        return model.toNotices()
    }

    fun readNotices(context: Context): Notices {
        val inputStream = context.assets.open(FILE_NAME)
        val notices = inputStream.reader().use {
            readFrom(it.readText())
        }
        return notices
    }

    const val FILE_NAME = "notices.json"
}

