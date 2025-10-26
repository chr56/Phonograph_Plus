/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.foundation.mediastore.EXTENDED_SONG_PROJECTION
import player.phonograph.foundation.mediastore.readSong
import player.phonograph.mechanism.explorer.Locations
import player.phonograph.model.file.FileItem
import player.phonograph.model.file.Location
import player.phonograph.repo.mediastore.internal.queryMediaFiles
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.file.audioFileFilter
import androidx.core.content.getSystemService
import android.content.Context
import android.database.Cursor
import android.os.storage.StorageManager
import android.provider.MediaStore
import kotlinx.coroutines.yield
import java.io.File

object MediaStoreFileEntities {

    /**
     * list files in [currentLocation] as format of FileItem via MediaStore
     */
    suspend fun listFilesMediaStore(
        context: Context,
        currentLocation: Location,
    ): List<FileItem> {
        val fileCursor = queryMediaFiles(
            context,
            "${MediaStore.MediaColumns.DATA} LIKE ?",
            arrayOf("${currentLocation.absolutePath}%")
        ) ?: return emptyList()
        val storageManager = context.getSystemService<StorageManager>()!!
        return fileCursor.use { cursor ->
            if (cursor.moveToFirst()) {
                val list = mutableListOf<FileItem>()
                do {
                    list.register(readFileItem(cursor, currentLocation, storageManager))
                } while (cursor.moveToNext())
                val sortMode = Setting(context)[Keys.fileSortMode].read()
                list.sortedWith(FileItem.SortedComparator(sortMode))
            } else emptyList()
        }
    }

    private fun MutableList<FileItem>.register(item: FileItem) {
        when (item.content) {
            is FileItem.FolderContent -> {
                val i = this.indexOf(item)
                if (i < 0) {
                    // unregistered
                    this.add(item.apply { item.content.count = 1 })
                } else {
                    // registered
                    (this[i].content as FileItem.FolderContent).count++
                }
            }

            else                      -> this.add(item)
        }
    }

    /**
     * Read audio file from non-empty [cursor] to [FileItem]
     * @param root root path location
     * @see [EXTENDED_SONG_PROJECTION]
     */
    private fun readFileItem(
        cursor: Cursor,
        root: Location,
        storageManager: StorageManager,
    ): FileItem {

        val song = readSong(cursor)

        val absolutePath = song.data
        val relativePath = absolutePath.substringAfter(root.absolutePath).removePrefix("/")

        return if (relativePath.contains('/')) {
            val folderName = relativePath.substringBefore('/')
            val folderPath = absolutePath.substringBefore(relativePath.substringAfter('/'))
            // folder
            FileItem(
                name = folderName,
                location = Locations.from(folderPath, storageManager),
                dateAdded = song.dateAdded,
                dateModified = song.dateModified,
                content = FileItem.FolderContent(0),
            )
        } else {
            val size = cursor.getLong(14)
            val displayName = cursor.getString(15)
            // file
            FileItem(
                name = displayName,
                location = Locations.from(song.data, storageManager),
                dateAdded = song.dateAdded,
                dateModified = song.dateModified,
                size = size,
                content = FileItem.SongContent(song),
            )
        }
    }


    /**
     * list files in [location] as format of FileItem via File API
     */
    suspend fun listFilesLegacy(
        context: Context,
        location: Location,
    ): List<FileItem> {
        val directory = File(location.absolutePath).also { if (!it.isDirectory) return emptyList() }
        val files = directory.listFiles(audioFileFilter) ?: return emptyList()
        if (files.isEmpty()) return emptyList()
        yield()
        val storageManager = context.getSystemService<StorageManager>()!!
        val result = files.map { file -> parse(file, storageManager) }
        val sortMode = Setting(context)[Keys.fileSortMode].read()
        return result.sortedWith(FileItem.SortedComparator(sortMode))
    }

    private fun parse(file: File, storageManager: StorageManager): FileItem {
        val location = Locations.from(file.absolutePath, storageManager)
        return FileItem(
            name = file.name,
            location = location,
            dateAdded = file.lastModified(),
            dateModified = file.lastModified(),
            size = file.length(),
            content = if (file.isDirectory) FileItem.FolderContent(-1) else FileItem.MediaContent,
        )
    }

}