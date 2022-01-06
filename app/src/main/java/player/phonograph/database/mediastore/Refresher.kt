/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.database.mediastore

import android.content.Context
import android.util.Log
import player.phonograph.helper.SortOrder
import player.phonograph.util.MediaStoreUtil
import player.phonograph.util.MediaStoreUtil.getSong
import player.phonograph.util.MediaStoreUtil.querySongs

object Refresher {

    fun importFromMediaStore(context: Context) {
        Log.i("RoomDatabase", "Start importing")

        val songs = MediaStoreUtil.getAllSongs(context)

        val songDataBaseDao = MusicDatabase.songsDataBase.SongDao()
        for (song in songs.listIterator()) {
            song?.let {
                songDataBaseDao.override(SongConverter.fromSongModel(it))
                Log.d("RoomDatabase", "Add Song:${it.title}")
            }
        }

        MusicDatabase.songsDataBase.lastUpdateTimestamp = getLastSong(context).dateModified
        Log.i("RoomDatabase", "End importing")
    }

    fun getLastSong(context: Context): player.phonograph.model.Song {

        return getSong(
            querySongs(
                context,
                null,
                null,
                SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT
            )
        )
    }
}
