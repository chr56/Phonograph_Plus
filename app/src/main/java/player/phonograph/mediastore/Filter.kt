/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore
import player.phonograph.provider.BlacklistStore

/**
 *  amend path with path filter SQL to SQLWhereClause
 */
fun withPathFilter(context: Context, block: () -> Pair<String, Array<String>>): Pair<String, Array<String>> {
    val paths = BlacklistStore.getInstance(context).paths.map { "$it%" }
    val target = block()
    if (paths.isNotEmpty()) {
        val realSelection =
            paths.fold(target.first.ifEmpty { "${MediaStore.MediaColumns.SIZE} > 0" }) { acc, _ -> "$acc AND ${MediaStore.Audio.AudioColumns.DATA} NOT LIKE ? " }
        val realSelectionValues =
            target.second.plus(paths)
        return Pair(realSelection, realSelectionValues)
    }
    return target
}