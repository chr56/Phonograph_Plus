package player.phonograph.loader

import android.content.Context
import player.phonograph.database.mediastore.MusicDatabase
import player.phonograph.database.mediastore.SongConverter
import player.phonograph.helper.SortOrder
import player.phonograph.model.Song
import player.phonograph.util.PreferenceUtil.Companion.getInstance

object LastAddedLoader {
    @JvmStatic
    fun getLastAddedSongs(context: Context): List<Song> {

        val list = MusicDatabase.songsDataBase.SongDao()
            .queryLastAddedSongs(
                getInstance(context).lastAddedCutoff, SortOrder.SongSortOrder.SONG_DATE_MODIFIED_REVERT
            )
        return List<Song>(list.size) { index ->
            SongConverter.toSongModel(list[index])
        }
    }
}
