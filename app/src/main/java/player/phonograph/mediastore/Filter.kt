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


    return if (paths.isNotEmpty()) {
        SQLWhereClause(
            plusSelectionCondition(target.selection, mode, pattern, paths.size),
            target.selectionValues.plus(paths)
        )
    } else {
        target
    }
}

/**
 * create duplicated string for selection
 * @param mode whitelist mode
 * @param what the string to duplicate
 * @param count count of the duplicated [what]
 */
private fun plusSelectionCondition(selection: String, mode: Boolean, what: String, count: Int): String {
    if (count <= 0) return selection // do nothing
    val separator = if (mode) "OR" else " AND"
    var accumulator = selection
    // first
    accumulator +=
        if (selection.isEmpty()) {
            "($what"
        } else {
            " AND ($what"
        }
    // rest
    for (i in 1 until count) {
        accumulator += " $separator $what"
    }
    accumulator += ")"
    return accumulator
}

inline fun withBaseAudioFilter(block: () -> String): String {
    val selection = block()
    return if (selection.isBlank()) {
        BASE_AUDIO_SELECTION
    } else {
        "$BASE_AUDIO_SELECTION AND $selection "
    }
}

inline fun withBasePlaylistFilter(block: () -> String?): String {
    val selection = block()
    return if (selection.isNullOrBlank()) {
        BASE_PLAYLIST_SELECTION
    } else {
        "$BASE_PLAYLIST_SELECTION AND $selection "
    }
}


