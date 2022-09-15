/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.module

import player.phonograph.BaseApp
import player.phonograph.model.sort.FileSortMode
import player.phonograph.model.sort.FileSortMode.Companion.deserialize
import player.phonograph.model.sort.SortRef

object FileSortModePreference {
    private const val FILE_SORT_MODE = "file_sort_mode"
    val defValue get() = FileSortMode(SortRef.ID, false).serialize()

    private val impl: StringIsolatePreference =
        StringIsolatePreference(FILE_SORT_MODE, defValue, BaseApp.instance)
    var fileSortMode: FileSortMode
        get() = deserialize(impl.read() ?: defValue)
        set(value) {
            impl.write(value.serialize())
        }
}