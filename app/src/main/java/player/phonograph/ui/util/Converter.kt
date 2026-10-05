/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.util

import player.phonograph.mechanism.playlist.PlaylistSongsActions
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.file.FileItem
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.Genres
import player.phonograph.repo.loader.Playlists
import player.phonograph.repo.loader.Songs
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Convert mixed item list, whose contains songs, to songs.
 */
suspend fun convertToSongs(context: Context, items: Iterable<*>): List<Song> = items.flatMap { item ->
    when (item) {
        is Song           -> listOf(item)
        is QueueSong      -> listOf(item.song)
        is Album          -> Songs.album(context, item.id)
        is Artist         -> Songs.artist(context, item.id)
        is Genre          -> Genres.songs(context, item.id)
        is Playlist       -> PlaylistSongsActions.reader(item).allSongs(context)
        is FileItem       -> convertToSongs(context, item)
        is SongCollection -> item.songs
        else              -> emptyList()
    }
}

suspend fun convertToSong(context: Context, file: FileItem): Song? =
    withContext(Dispatchers.IO) {
        if (file.content is FileItem.SongContent) {
            file.content.song
        } else if (file.mediaPath.mediastoreId > 0) {
            Songs.id(context, file.mediaPath.mediastoreId)
        } else {
            Songs.path(context, file.path, withoutPathFilter = true)
        }
    }

suspend fun convertToSongs(context: Context, file: FileItem): List<Song> =
    withContext(Dispatchers.IO) {
        when (val content = file.content) {
            is FileItem.SongContent     -> listOf(content.song)
            is FileItem.PlaylistContent -> Playlists.songs(context, content.playlist.location).map { it.song }
            is FileItem.FolderContent   -> Songs.searchByPath(context, file.path, false)
            else                        -> emptyList()
        }
    }