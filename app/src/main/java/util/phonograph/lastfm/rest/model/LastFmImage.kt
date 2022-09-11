/*
 * Copyright (c) 2022 chr_56
 */

package util.phonograph.lastfm.rest.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class LastFmImage {
    @SerialName("#text")
    var text: String = ""
    var size: String = ""
}