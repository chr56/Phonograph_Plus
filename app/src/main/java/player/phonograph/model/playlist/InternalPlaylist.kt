/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.model.playlist

import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.converter.MediastoreSongConverter
import androidx.annotation.DrawableRes
import android.content.Context

abstract class InternalPlaylist : Playlist(), EditablePlaylist {


    override val type: Int
        get() = PlaylistType.DATABASE

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.ic_queue_music_white_24dp

    override suspend fun getSongs(context: Context): List<Song> {
        val database = MusicDatabase.database(context)
        val entity = database.PlaylistSongDao().playlist(id)
        return entity?.songs?.map { songEntity ->
            if (songEntity != null) {
                MediastoreSongConverter.toSongModel(songEntity)
            } else {
                Song.EMPTY_SONG
            }
        } ?: emptyList()
    }

    override suspend fun containsSong(context: Context, songId: Long): Boolean {
        val database = MusicDatabase.database(context)
        val entity = database.PlaylistSongDao().playlist(id)
        return entity?.songs?.firstOrNull { it?.mediastorId == songId } != null
    }

}