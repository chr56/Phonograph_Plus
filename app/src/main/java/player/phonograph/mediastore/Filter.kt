/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore
import player.phonograph.provider.BlacklistStore

class SQLWhereClause(val selection: String, val selectionValues: Array<String>)

/**
 *  amend path with path filter SQL to SQLWhereClause
 */
fun withPathFilter(context: Context, block: () -> SQLWhereClause): SQLWhereClause {
    val paths = BlacklistStore.getInstance(context).paths.map { "$it%" }
    val target = block()
    return if (paths.isNotEmpty()) {
        SQLWhereClause(
            paths.fold(target.selection.ifEmpty { "${MediaStore.MediaColumns.SIZE} > 0" }) { acc, _ -> "$acc AND ${MediaStore.Audio.AudioColumns.DATA} NOT LIKE ? " },
            target.selectionValues.plus(paths)
        )
    } else {
        target
    }
}

const val BASE_AUDIO_SELECTION =
    "${MediaStore.Audio.AudioColumns.IS_MUSIC} =1 "

inline fun withBaseAudioFilter(block: () -> String?): String {
    val selection = block()
    return if (selection.isNullOrBlank()) {
        BASE_AUDIO_SELECTION
    } else {
        "$BASE_AUDIO_SELECTION AND $selection "
    }
}

const val BASE_PLAYLIST_SELECTION =
    "${MediaStore.Audio.PlaylistsColumns.NAME} != '' "

inline fun withBasePlaylistFilter(block: () -> String?): String {
    val selection = block()
    return if (selection.isNullOrBlank()) {
        BASE_PLAYLIST_SELECTION
    } else {
        "$BASE_PLAYLIST_SELECTION AND $selection "
    }
}


