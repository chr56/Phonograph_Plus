package player.phonograph.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import androidx.annotation.NonNull;

import player.phonograph.mediastore.SongLoader;
import player.phonograph.model.Song;
import player.phonograph.settings.Setting;

import java.util.List;

public class LastAddedLoader {

    @NonNull
    public static List<Song> getLastAddedSongs(@NonNull Context context) {
        return SongLoader.getSongs(makeLastAddedCursor(context));
    }

    public static Cursor makeLastAddedCursor(@NonNull final Context context) {
        long cutoff = Setting.instance().getLastAddedCutoff();

        return SongLoader.makeSongCursor(
                context,
                MediaStore.Audio.Media.DATE_ADDED + ">?",
                new String[]{String.valueOf(cutoff)},
                MediaStore.Audio.Media.DATE_ADDED + " DESC");
    }
}
