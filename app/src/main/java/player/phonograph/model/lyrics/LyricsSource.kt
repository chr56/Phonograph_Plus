/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics

import player.phonograph.R
import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class LyricsSource : Parcelable {
    Embedded,
    ExternalPrecise,
    ExternalDecorated,
    ManuallyLoaded,
    Unknown;

    fun name(context: Context): String = when (this) {
        Embedded                           -> context.getString(R.string.label_embedded_lyrics)
        ExternalDecorated, ExternalPrecise -> context.getString(R.string.label_external_lyrics)
        ManuallyLoaded                     -> context.getString(R.string.label_loaded)
        Unknown                            -> "N/A"
    }
}