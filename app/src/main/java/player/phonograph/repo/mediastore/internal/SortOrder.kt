/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore.internal

import player.phonograph.foundation.mediastore.mediastoreSongQuerySortRef
import player.phonograph.model.sort.SortMode
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

fun defaultSongQuerySortOrder(context: Context) =
    mediastoreSongQuerySortOrder(Setting(context)[Keys.songSortMode].data)

fun mediastoreSongQuerySortOrder(sortMode: SortMode): String =
    "${mediastoreSongQuerySortRef(sortMode.sortRef)} ${if (sortMode.revert) "DESC" else "ASC"}"
