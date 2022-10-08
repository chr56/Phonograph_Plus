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
 *  @param mode whitelist mode
 */
fun withPathFilter(context: Context, mode: Boolean = false, block: () -> SQLWhereClause): SQLWhereClause {
    val paths =
        if (mode)
            emptyList() // todo: not implemented yet
        else
            BlacklistStore.getInstance(context).paths.map { "$it%" }

    val target = block()

    val pattern =
        if (mode)
            "${MediaStore.Audio.AudioColumns.DATA} LIKE ?"
        else
            "${MediaStore.Audio.AudioColumns.DATA} NOT LIKE ?"

    val separator = if (mode) "OR" else " AND"

    return if (paths.isNotEmpty()) {
        SQLWhereClause(
            plusSelectionCondition(target.selection, pattern, paths.size, separator),
            target.selectionValues.plus(paths)
        )
    } else {
        target
    }
}

/**
 * create duplicated string for selection
 * @param what the string to duplicate
 * @param count count of the duplicated [what]
 * @param separator separator
 */
private fun plusSelectionCondition(selection: String, what: String, count: Int, separator: String): String {
    var accumulator = selection
    // first
    accumulator +=
        if (selection.isEmpty()) {
            what
        } else {
            " $separator $what"
        }
    // rest
    for (i in 1 until count) {
        accumulator += " $separator $what"
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


