/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.actions

import player.phonograph.model.Song
import player.phonograph.model.file.FileItem
import player.phonograph.repo.loader.Playlists
import player.phonograph.repo.loader.Songs
import player.phonograph.util.asList
import android.content.Context

suspend fun FileItem.songs(context: Context): List<Song> =
    when (content) {
        is FileItem.SongContent     -> content.song.asList()
        is FileItem.PlaylistContent -> Playlists.songs(context, content.playlist.location).map { it.song }
        is FileItem.FolderContent   -> Songs.searchByPath(context, "%${location.absolutePath}%", false)
        else                        -> emptyList()
    }