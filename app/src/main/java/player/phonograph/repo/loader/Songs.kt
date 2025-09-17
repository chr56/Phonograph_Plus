/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.repo.loader

import player.phonograph.model.Song
import player.phonograph.model.repo.loader.Delegated
import player.phonograph.model.repo.loader.ISongs
import player.phonograph.repo.mediastore.MediaStoreSongs
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

/**
 * Endpoint for accessing songs
 */
object Songs : ISongs, Delegated<ISongs>() {

    override fun onCreateDelegate(context: Context): ISongs {
        val preference = Setting(context)[Keys.musicLibraryBackend]
        val impl: ISongs = when (preference.data) {
            else                       -> MediaStoreSongs
        }
        return impl
    }

    override suspend fun all(context: Context): List<Song> =
        delegate(context).all(context)

    override suspend fun id(context: Context, id: Long): Song? =
        delegate(context).id(context, id)

    override suspend fun path(context: Context, path: String): Song? =
        delegate(context).path(context, path)

    override suspend fun artist(context: Context, artistId: Long): List<Song> =
        delegate(context).artist(context, artistId)

    override suspend fun album(context: Context, albumId: Long): List<Song> =
        delegate(context).album(context, albumId)

    override suspend fun searchByPath(context: Context, path: String, withoutPathFilter: Boolean): List<Song> =
        delegate(context).searchByPath(context, path, withoutPathFilter)

    override suspend fun searchByTitle(context: Context, title: String): List<Song> =
        delegate(context).searchByTitle(context, title)

    override suspend fun since(context: Context, timestamp: Long, useModifiedDate: Boolean): List<Song> =
        delegate(context).since(context, timestamp, useModifiedDate)

}