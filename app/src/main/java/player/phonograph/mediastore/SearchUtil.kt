/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.mediastore

import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.model.file.put
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive

/**
 * This might be time-consuming
 * @param currentLocation the location you want to query
 * @param scope CoroutineScope (Optional)
 * @return the ordered TreeSet containing songs and folders in this location
 */
fun searchSongFiles(context: Context, currentLocation: Location, scope: CoroutineScope? = null): Set<FileEntity>? {
    val fileCursor = querySongFiles(
        context,
        "${MediaStore.MediaColumns.DATA} LIKE ?",
        arrayOf("${currentLocation.absolutePath}%"),
    ) ?: return null
    return fileCursor.use { cursor ->
        if (cursor.moveToFirst()) {
            val list: MutableList<FileEntity> = ArrayList()
            do {
                if (scope?.isActive == false) break
                val item = parseFileEntity(cursor, currentLocation)
                list.put(item)
            } while (cursor.moveToNext())
            list.toSortedSet()
        } else null
    }
}