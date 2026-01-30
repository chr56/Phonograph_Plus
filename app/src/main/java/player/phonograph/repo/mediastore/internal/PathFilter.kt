/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.internal

import player.phonograph.settings.Keys
import player.phonograph.settings.PathFilterSetting
import player.phonograph.settings.Setting
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore

/**
 *  Wrap query selection with paths from path filter by Settings
 *  @param escape true if disable path filter
 *  @param block produce the cursor with provided selection with path filter
 *  @return produced cursor
 */
internal suspend inline fun withPathFilter(
    context: Context,
    selection: String = "",
    selectionValues: Array<String> = emptyArray(),
    escape: Boolean = false,
    block: (selection: String, selectionValues: Array<String>) -> Cursor?,
): Cursor? {

    if (escape) return block(selection, selectionValues)

    val includeMode = !Setting(context)[Keys.pathFilterExcludeMode].read()

    val paths = PathFilterSetting(!includeMode).read(context).map { "$it%" }


    val pattern =
        if (includeMode)
            "${MediaStore.Audio.AudioColumns.DATA} LIKE ?"
        else
            "${MediaStore.Audio.AudioColumns.DATA} NOT LIKE ?"


    return if (paths.isEmpty()) {
        block(
            selection,
            selectionValues
        )
    } else {
        block(
            withSelectionCondition(selection, includeMode, pattern, paths.size),
            selectionValues.plus(paths),
        )
    }
}


/**
 * create duplicated string for selection
 * @param includeMode "whitelist" mode
 * @param what the string to duplicate
 * @param count count of the duplicated [what]
 */
internal fun withSelectionCondition(selection: String, includeMode: Boolean, what: String, count: Int): String {
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

