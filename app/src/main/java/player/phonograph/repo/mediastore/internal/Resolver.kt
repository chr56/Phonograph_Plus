/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.mediastore.internal

import player.phonograph.foundation.mediastore.BASE_FILE_PROJECTION
import player.phonograph.mechanism.explorer.Locations
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import android.annotation.SuppressLint
import android.database.Cursor
import android.os.storage.StorageManager


/**
 * read audio file cursor as [FileEntity]
 * (**require [cursor] not empty**)
 * @param currentLocation location where treats as base
 * @see [BASE_FILE_PROJECTION]
 */
@SuppressLint("Range")
fun readFileEntity(
    cursor: Cursor,
    currentLocation: Location,
    storageManager: StorageManager,
): FileEntity {
    val id = cursor.getLong(0)
    val displayName = cursor.getString(1)
    val absolutePath = cursor.getString(2)
    val size = cursor.getLong(3)
    val dateAdded = cursor.getLong(4)
    val dateModified = cursor.getLong(5)

    val relativePath = absolutePath.stripToRelativePath(currentLocation.absolutePath)

    return if (relativePath.contains('/')) {
        val folderName = relativePath.substringBefore('/')
        val folderPath = absolutePath.substringBefore(relativePath.substringAfter('/'))
        // folder
        FileEntity.Folder(
            location = Locations.from(folderPath, storageManager),
            name = folderName,
            dateAdded = dateAdded,
            dateModified = dateModified
        )
    } else {
        // file
        FileEntity.File(
            location = Locations.from(absolutePath, storageManager),
            name = displayName,
            id = id,
            size = size,
            dateAdded = dateAdded,
            dateModified = dateModified
        )
    }
}

private fun String.stripToRelativePath(currentLocationAbsolutePath: String) =
    substringAfter(currentLocationAbsolutePath).removePrefix("/")