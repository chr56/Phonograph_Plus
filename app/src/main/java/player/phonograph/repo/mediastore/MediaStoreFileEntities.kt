/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.mediastore

import player.phonograph.mechanism.explorer.Locations
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.internal.querySongFiles
import player.phonograph.repo.mediastore.internal.readFileEntity
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.file.audioFileFilter
import androidx.core.content.getSystemService
import android.content.Context
import android.os.storage.StorageManager
import android.provider.MediaStore
import kotlinx.coroutines.yield
import java.io.File

object MediaStoreFileEntities {

    /**
     * list files in [currentLocation] as format of FileEntity via MediaStore
     */
    suspend fun listFilesMediaStore(
        context: Context,
        currentLocation: Location,
    ): List<FileEntity> {
        val fileCursor = querySongFiles(
            context,
            "${MediaStore.MediaColumns.DATA} LIKE ?",
            arrayOf("${currentLocation.absolutePath}%")
        ) ?: return emptyList()
        val storageManager = context.getSystemService<StorageManager>()!!
        return fileCursor.use { cursor ->
            if (cursor.moveToFirst()) {
                val list: MutableList<FileEntity> = ArrayList()
                do {
                    val item = readFileEntity(cursor, currentLocation, storageManager)
                    yield()
                    list.put(item)
                } while (cursor.moveToNext())
                val sortMode = Setting(context)[Keys.fileSortMode].read()
                list.sortedWith(FileEntityComparator(sortMode))
            } else emptyList()
        }
    }

    private fun MutableList<FileEntity>.put(item: FileEntity) {
        when (item) {
            is FileEntity.File   -> this.add(item)
            is FileEntity.Folder -> {
                // count songs for folder
                val i = this.indexOf(item)
                if (i < 0) {
                    this.add(item.apply { songCount = 1 })
                } else {
                    (this[i] as FileEntity.Folder).songCount++
                }
            }
        }
    }

    /**
     * list files in [location] as format of FileEntity via File API
     */
    suspend fun listFilesLegacy(
        context: Context,
        location: Location,
    ): List<FileEntity> {
        val directory = File(location.absolutePath).also { if (!it.isDirectory) return emptyList() }
        val files = directory.listFiles(audioFileFilter) ?: return emptyList()
        yield()
        val result = ArrayList<FileEntity>()
        val storageManager = context.getSystemService<StorageManager>()!!
        for (file in files) {
            val item = readFile(file, storageManager)
            yield()
            if (item != null) result.add(item)
        }
        val sortMode = Setting(context)[Keys.fileSortMode].read()
        return result.sortedWith(FileEntityComparator(sortMode))
    }

    private fun readFile(file: File, storageManager: StorageManager): FileEntity? {
        val location = Locations.from(file.absolutePath, storageManager)
        return when {
            file.isDirectory -> {
                FileEntity.Folder(
                    location = location,
                    name = file.name,
                    dateAdded = file.lastModified(),
                    dateModified = file.lastModified()
                )
            }

            file.isFile      -> {
                FileEntity.File(
                    location = location,
                    name = file.name,
                    size = file.length(),
                    dateAdded = file.lastModified(),
                    dateModified = file.lastModified()
                )
            }

            else             -> null
        }
    }

    private class FileEntityComparator(val currentSortRef: SortMode) : Comparator<FileEntity> {
        override fun compare(a: FileEntity?, b: FileEntity?): Int {
            if (a == null || b == null) return 0
            return if ((a is FileEntity.Folder) xor (b is FileEntity.Folder)) {
                if (a is FileEntity.Folder) -1 else 1
            } else {
                when (currentSortRef.sortRef) {
                    SortRef.MODIFIED_DATE -> a.dateModified.compareTo(b.dateModified)
                    SortRef.ADDED_DATE    -> a.dateAdded.compareTo(b.dateAdded)
                    SortRef.SIZE          -> {
                        if (a is FileEntity.File && b is FileEntity.File) a.size.compareTo(b.size)
                        else a.name.compareTo(b.name)
                    }

                    else                  -> a.name.compareTo(b.name)
                }.let {
                    if (currentSortRef.revert) -it else it
                }
            }
        }
    }
}