package player.phonograph.loader

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import player.phonograph.loader.SongLoader.getSongs
import player.phonograph.loader.SongLoader.makeSongCursor
import player.phonograph.model.Song
import player.phonograph.util.PreferenceUtil.Companion.getInstance

object LastAddedLoader {
    @JvmStatic
    fun getLastAddedSongs(context: Context): List<Song> {
        return getSongs(makeLastAddedCursor(context))
    }

    private fun makeLastAddedCursor(context: Context): Cursor? {
        val cutoff = getInstance(context).lastAddedCutoff
        return makeSongCursor(
            context,
            MediaStore.Audio.Media.DATE_ADDED + ">?", arrayOf(cutoff.toString()),
            MediaStore.Audio.Media.DATE_ADDED + " DESC"
        )
    }
}
