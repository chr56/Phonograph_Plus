package com.kabouzeid.gramophone.helper;

import android.content.Context;

import com.kabouzeid.gramophone.loader.PlaylistSongLoader;
import com.kabouzeid.gramophone.model.AbsCustomPlaylist;
import com.kabouzeid.gramophone.model.Playlist;
import com.kabouzeid.gramophone.model.Song;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class M3UWriter implements M3UConstants {

    public static File write(Context context, File dir, Playlist playlist) throws IOException {
        if (!dir.exists()) //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        //File name
        String filename;

        List<? extends Song> songs;
        if (playlist instanceof AbsCustomPlaylist) {
            songs = ((AbsCustomPlaylist) playlist).getSongs(context);
            // Since AbsCustomPlaylists are dynamic, we add a timestamp after their names.
            filename = playlist.name.concat(new SimpleDateFormat("_yy-MM-dd_HH-mm", Locale.getDefault()).format(Calendar.getInstance().getTime()));
        } else {
            songs = PlaylistSongLoader.getPlaylistSongList(context, playlist.id);
            filename = playlist.name;
        }

        File file = new File(dir, filename.concat("." + EXTENSION));

        if (songs.size() > 0) {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            bw.write(HEADER);
            for (Song song : songs) {
                bw.newLine();
                bw.write(ENTRY + song.duration + DURATION_SEPARATOR + song.artistName + " - " + song.title);
                bw.newLine();
                bw.write(song.data);
            }

            bw.close();
        }

        return file;
    }
}
