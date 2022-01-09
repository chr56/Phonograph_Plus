package player.phonograph.loader

import android.content.Context
import player.phonograph.database.mediastore.MusicDatabase
import player.phonograph.helper.ModelConverterHelper
import player.phonograph.model.Song
import player.phonograph.util.PreferenceUtil.Companion.getInstance

object LastAddedLoader {
    @JvmStatic
    fun getLastAddedSongs(context: Context): List<Song> {

        val list = MusicDatabase.songsDataBase.SongDao()
            .queryLastAddedSongs(getInstance(context).lastAddedCutoff)
        return ModelConverterHelper.convertSong(list)
    }
}
