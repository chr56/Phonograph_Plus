/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.database.mediastore

import android.content.Context
import player.phonograph.util.MediaStoreUtil

object Refresher {

    fun importFromMediaStore(context: Context) {
        val songs = MediaStoreUtil.getAllSongs(context)

        val songDataBaseDao = MusicDatabase.songsDataBase.SongDao()
        for (song in songs.listIterator()) {
            song?.let {
                songDataBaseDao.override(SongConverter.fromSongModel(it))
            }
        }
    }
}
