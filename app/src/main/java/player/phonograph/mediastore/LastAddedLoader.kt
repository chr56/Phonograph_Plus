/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mediastore

import player.phonograph.model.Song
import player.phonograph.settings.Setting
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore

object LastAddedLoader {
    fun getLastAddedSongs(context: Context): List<Song> {
        return makeLastAddedCursor(context).intoSongs()
    }

    private fun makeLastAddedCursor(context: Context): Cursor? {
        val cutoff = Setting.instance.lastAddedCutoff
        return querySongs(
            context,
            MediaStore.Audio.Media.DATE_ADDED + ">?",
            arrayOf(cutoff.toString()),
            MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )
    }
}
