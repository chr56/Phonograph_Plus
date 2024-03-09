/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import android.content.Context

interface ISongs {

    suspend fun all(context: Context): List<Song>

    suspend fun id(context: Context, id: Long): Song

    suspend fun path(context: Context, path: String): Song

    suspend fun artist(context: Context, artistId: Long): List<Song>

    suspend fun album(context: Context, albumId: Long): List<Song>

    suspend fun genres(context: Context, genreId: Long): List<Song>

    /**
     * @param withoutPathFilter true if disable path filter
     */
    suspend fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song>

    suspend fun searchByTitle(context: Context, title: String): List<Song>

    suspend fun searchByFileEntity(context: Context, file: FileEntity.File): Song

    suspend fun since(context: Context, timestamp: Long, useModifiedDate: Boolean = false): List<Song>

}