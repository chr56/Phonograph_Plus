/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.version

import androidx.annotation.Keep
import android.os.Parcelable
import kotlin.collections.maxByOrNull
import kotlinx.parcelize.Parcelize


@Keep
@kotlinx.serialization.Serializable
@Parcelize
class VersionCatalog(
    val versions: List<Version> = emptyList(),
) : Parcelable {

    fun filter(selector: (Version) -> Boolean): List<Version> = versions.filter(selector)

    fun filter(channel: String): List<Version> = filter { version -> version.channel == channel }

    val latest: Version? get() = versions.maxByOrNull { version -> version.versionCode }

    fun latest(channel: String): Version? = filter(channel).maxByOrNull { it.date }
}