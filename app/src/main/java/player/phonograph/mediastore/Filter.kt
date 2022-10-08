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
            plusBlacklistSelectionCondition(target.selection, paths.size),
            target.selectionValues.plus(paths)
        )
    } else {
        target
    }
}

/**
 * create placeholder in selection
 * @param count count of the duplicated
 */
private fun plusBlacklistSelectionCondition(selection: String, count: Int): String {
    val what = "${MediaStore.Audio.AudioColumns.DATA} NOT LIKE ?"
    var accumulator = selection
    // first
    accumulator +=
        if (selection.isEmpty()) {
            what
        } else {
            "AND $what"
        }
    // rest
    for (i in 1 until count) {
        accumulator += "AND $what"
    }
    return accumulator
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


