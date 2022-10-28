/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore
import player.phonograph.provider.PathFilterStore
import player.phonograph.settings.Setting

class SQLWhereClause(val selection: String, val selectionValues: Array<String>)

/**
 *  amend path with path filter SQL to SQLWhereClause
 */
fun withPathFilter(context: Context, block: () -> SQLWhereClause): SQLWhereClause {
    val includeMode = !Setting.instance(context).pathFilterExcludeMode

    val paths =
        if (includeMode)
            PathFilterStore.getInstance(context).whitelistPaths.map { "$it%" }
        else
            PathFilterStore.getInstance(context).blacklistPaths.map { "$it%" }

    val target = block()

    val pattern =
        if (includeMode)
            "${MediaStore.Audio.AudioColumns.DATA} LIKE ?"
        else
            "${MediaStore.Audio.AudioColumns.DATA} NOT LIKE ?"


    return if (paths.isNotEmpty()) {
        SQLWhereClause(
            plusSelectionCondition(target.selection, includeMode, pattern, paths.size),
            target.selectionValues.plus(paths)
        )
    } else {
        target
    }
}

/**
 * create duplicated string for selection
 * @param includeMode "whitelist" mode
 * @param what the string to duplicate
 * @param count count of the duplicated [what]
 */
private fun plusSelectionCondition(selection: String, includeMode: Boolean, what: String, count: Int): String {
    if (count <= 0) return selection // do nothing
    val separator = if (includeMode) "OR" else " AND"
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


