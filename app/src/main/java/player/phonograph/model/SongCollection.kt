/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SongCollection(
    val name: String,
    val songs: List<Song>,
    val detail: String? = null,
) : Parcelable