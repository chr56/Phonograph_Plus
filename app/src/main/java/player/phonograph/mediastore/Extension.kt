/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore
import player.phonograph.provider.BlacklistStore

/**
 *  amend path blacklist filter SQL codes to Pair(selection, selectionValues)
 */
fun Pair<String, Array<String>>.generateBlacklistFilter(context: Context): Pair<String, Array<String>> {
    val paths: List<String> = BlacklistStore.getInstance(context).paths
    if (paths.isNotEmpty()) {
        val realSelection =
            paths.fold(first.ifEmpty { "${MediaStore.MediaColumns.SIZE} > 0" }) { acc, _ -> "$acc AND ${MediaStore.Audio.AudioColumns.DATA} NOT LIKE ? " }
        val realSelectionValues =
            second.plus(paths)
        return Pair(realSelection, realSelectionValues)
    }
    return this
}
