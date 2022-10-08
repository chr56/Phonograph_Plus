/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.mediastore

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.model.file.put

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
                val item = parseFileEntity(cursor, currentLocation)
                list.put(item)
            } while (cursor.moveToNext())
            list.toSortedSet()
        } else null
    }
}

fun searchSongs(context: Context, currentLocation: Location, scope: CoroutineScope? = null): List<Song> {
    val cursor = querySongs(
        context, "${MediaStore.MediaColumns.DATA} LIKE ?", arrayOf("%${currentLocation.absolutePath}%")
    )
    return MediaStoreUtil.getSongs(cursor)
}

fun searchSong(context: Context, fileName: String): Song {
    val cursor = querySongs(
        context,
        "${MediaStore.MediaColumns.DATA} LIKE ? OR ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? ",
        arrayOf(fileName, fileName)
    )
    return MediaStoreUtil.getSong(cursor)
}