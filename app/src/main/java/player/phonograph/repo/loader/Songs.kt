/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.repo.mediastore.loaders.SongLoader
import android.content.Context

object Songs {

    fun all(context: Context): List<Song> = SongLoader.all(context)

    fun id(context: Context, id: Long): Song = SongLoader.id(context, id)

    fun path(context: Context, path: String): Song = SongLoader.path(context, path)

    /**
     * @param withoutPathFilter true if disable path filter
     */
    fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song> =
        SongLoader.searchByPath(context, path, withoutPathFilter)

    fun searchByTitle(context: Context, title: String): List<Song> =
        SongLoader.searchByTitle(context, title)

    fun searchByFileEntity(context: Context, file: FileEntity.File): Song =
        SongLoader.searchByFileEntity(context, file)

    fun since(context: Context, timestamp: Long, useModifiedDate: Boolean = false): List<Song> =
        SongLoader.since(context, timestamp, useModifiedDate)

}