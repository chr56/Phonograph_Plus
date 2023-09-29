/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Album
import android.content.Context

interface IAlbums {

    fun all(context: Context): List<Album>

    fun id(context: Context, id: Long): Album

    fun searchByName(context: Context, query: String): List<Album>

    fun artist(context: Context, artistId: Long): List<Album>

}