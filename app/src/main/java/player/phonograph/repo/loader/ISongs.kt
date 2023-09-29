/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import android.content.Context

interface ISongs {

    fun all(context: Context): List<Song>

    fun id(context: Context, id: Long): Song

    fun path(context: Context, path: String): Song

    fun artist(context: Context, artistId: Long): List<Song>

    fun album(context: Context, albumId: Long): List<Song>

    fun genres(context: Context, genreId: Long): List<Song>

    /**
     * @param withoutPathFilter true if disable path filter
     */
    fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song>

    fun searchByTitle(context: Context, title: String): List<Song>

    fun searchByFileEntity(context: Context, file: FileEntity.File): Song

    fun since(context: Context, timestamp: Long, useModifiedDate: Boolean = false): List<Song>

}