/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.loaders

import player.phonograph.App
import player.phonograph.mechanism.explorer.Locations
import player.phonograph.mechanism.scanner.FileScanner
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.repo.mediastore.internal.querySongFiles
import player.phonograph.repo.mediastore.internal.readFileEntity
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import androidx.core.content.getSystemService
import android.content.Context
import android.os.storage.StorageManager
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import java.io.File

object FileEntityLoader {

    /**
     * list files in [currentLocation] as format of FileEntity via MediaStore
     */
    fun listFilesMediaStore(
        currentLocation: Location,
        context: Context,
        scope: CoroutineScope?,
    ): List<FileEntity> {
        val fileCursor = querySongFiles(
            context,
            "${MediaStore.MediaColumns.DATA} LIKE ?",
            arrayOf("${currentLocation.absolutePath}%"),
        ) ?: return emptyList()
        val storageManager = context.getSystemService<StorageManager>()!!
        return fileCursor.use { cursor ->
            if (cursor.moveToFirst()) {
                val list: MutableList<FileEntity> = ArrayList()
                do {
                    if (scope?.isActive == false) break
                    val item = readFileEntity(cursor, currentLocation, storageManager)
                    list.put(item)
                } while (cursor.moveToNext())
                val sortMode = Setting(App.instance)[Keys.fileSortMode].data
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
    fun listFilesLegacy(
        location: Location,
        context: Context,
        scope: CoroutineScope?,
    ): List<FileEntity> {
        val directory = File(location.absolutePath).also { if (!it.isDirectory) return emptyList() }
        val files = directory.listFiles(FileScanner.audioFileFilter) ?: return emptyList()
        val result = ArrayList<FileEntity>()
        val storageManager = context.getSystemService<StorageManager>()!!
        for (file in files) {
            val l = Locations.from(file.absolutePath, storageManager)
            if (scope?.isActive == false) break
            val item =
                when {
                    file.isDirectory -> {
                        FileEntity.Folder(
                            location = l,
                            name = file.name,
                            dateAdded = file.lastModified(),
                            dateModified = file.lastModified()
                        )
                    }

                    file.isFile      -> {
                        FileEntity.File(
                            location = l,
                            name = file.name,
                            size = file.length(),
                            dateAdded = file.lastModified(),
                            dateModified = file.lastModified()
                        )
                    }

                    else             -> null
                }
            item?.let { result.add(it) }
        }
        val sortMode = Setting(context)[Keys.fileSortMode].data
        return result.sortedWith(FileEntityComparator(sortMode))
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