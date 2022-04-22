package player.phonograph.loader

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import player.phonograph.mediastore.SongLoader
import player.phonograph.mediastore.SongLoader.getSongs
import player.phonograph.model.Song
import player.phonograph.settings.Setting

object LastAddedLoader {
    fun getLastAddedSongs(context: Context): List<Song> {
        return getSongs(makeLastAddedCursor(context))
    }

    fun makeLastAddedCursor(context: Context): Cursor? {
        val cutoff = Setting.instance.lastAddedCutoff
        return SongLoader.makeSongCursor(
            context, MediaStore.Audio.Media.DATE_ADDED + ">?", arrayOf(cutoff.toString()), MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )
    }
}
