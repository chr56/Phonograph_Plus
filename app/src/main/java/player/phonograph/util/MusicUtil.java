package player.phonograph.util;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import player.phonograph.App;
import player.phonograph.R;
import player.phonograph.mediastore.GenreLoader;
import player.phonograph.model.Album;
import player.phonograph.model.Artist;
import player.phonograph.model.Genre;
import player.phonograph.model.Song;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class MusicUtil {

    public static Uri getMediaStoreAlbumCoverUri(long albumId) {
        final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");

        return ContentUris.withAppendedId(sArtworkUri, albumId);
    }

    public static Uri getSongFileUri(long songId) {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
    }

    @NonNull
    public static Intent createShareSongFileIntent(@NonNull final Song song, Context context) {
        try {
            return new Intent()
                    .setAction(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName(), new File(song.data)))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setType("audio/*");
        } catch (IllegalArgumentException e) {
            // TODO the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
            e.printStackTrace();
            Toast.makeText(context, "Could not share this file, I'm aware of the issue.", Toast.LENGTH_SHORT).show();
            return new Intent();
        }
    }


    @NonNull
    public static String getArtistInfoString(@NonNull final Context context, @NonNull final Artist artist) {
        int albumCount = artist.getAlbumCount();
        int songCount = artist.getSongCount();

        return MusicUtil.buildInfoString(
                MusicUtil.getAlbumCountString(context, albumCount),
                MusicUtil.getSongCountString(context, songCount)
        );
    }

    @NonNull
    public static String getAlbumInfoString(@NonNull final Context context, @NonNull final Album album) {
        int songCount = album.getSongCount();

        return MusicUtil.buildInfoString(
                album.getArtistName(),
                MusicUtil.getSongCountString(context, songCount)
        );
    }

    @NonNull
    public static String getSongInfoString(@NonNull final Song song) {
        return MusicUtil.buildInfoString(
                song.artistName,
                song.albumName
        );
    }

    @NonNull
    public static String getGenreInfoString(@NonNull final Context context, @NonNull final Genre genre) {
        int songCount = genre.songCount;
        return MusicUtil.getSongCountString(context, songCount);
    }

    @NonNull
    public static String getPlaylistInfoString(@NonNull final Context context, @NonNull List<Song> songs) {
        final long duration = getTotalDuration(context, songs);

        return MusicUtil.buildInfoString(
                MusicUtil.getSongCountString(context, songs.size()),
                MusicUtil.getReadableDurationString(duration)
        );
    }

    @NonNull
    public static String getSongCountString(@NonNull final Context context, int songCount) {
        final String songString = songCount == 1 ? context.getResources().getString(R.string.song) : context.getResources().getString(R.string.songs);
        return songCount + " " + songString;
    }

    @NonNull
    public static String getAlbumCountString(@NonNull final Context context, int albumCount) {
        final String albumString = albumCount == 1 ? context.getResources().getString(R.string.album) : context.getResources().getString(R.string.albums);
        return albumCount + " " + albumString;
    }

    @NonNull
    public static String getYearString(int year) {
        return year > 0 ? String.valueOf(year) : "-";
    }

    public static long getTotalDuration(@NonNull final Context context, @NonNull List<Song> songs) {
        long duration = 0;
        for (int i = 0; i < songs.size(); i++) {
            duration += songs.get(i).duration;
        }
        return duration;
    }

    public static String getReadableDurationString(long songDurationMillis) {
        long minutes = (songDurationMillis / 1000) / 60;
        long seconds = (songDurationMillis / 1000) % 60;
        if (minutes < 60) {
            return String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds);
        } else {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
    }

    /**
     * Build a concatenated string from the provided arguments
     * The intended purpose is to show extra annotations
     * to a music library item.
     * Ex: for a given album --> buildInfoString(album.artist, album.songCount)
     */
    @NonNull
    public static String buildInfoString(@Nullable final String string1, @Nullable final String string2) {
        // Skip empty strings
        if (TextUtils.isEmpty(string1)) {
            //noinspection ConstantConditions
            return TextUtils.isEmpty(string2) ? "" : string2;
        }
        if (TextUtils.isEmpty(string2)) {
            //noinspection ConstantConditions
            return TextUtils.isEmpty(string1) ? "" : string1;
        }

        return string1 + "  •  " + string2;
    }

    //iTunes uses for example 1002 for track 2 CD1 or 3011 for track 11 CD3.
    //this method converts those values to normal tracknumbers
    public static int getFixedTrackNumber(int trackNumberToFix) {
        return trackNumberToFix % 1000;
    }

    public static void insertAlbumArt(@NonNull Context context, long albumId, String path) {
        ContentResolver contentResolver = context.getContentResolver();

        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
        contentResolver.delete(ContentUris.withAppendedId(artworkUri, albumId), null, null);

        ContentValues values = new ContentValues();
        values.put("album_id", albumId);
        values.put("_data", path);

        contentResolver.insert(artworkUri, values);
        contentResolver.notifyChange(artworkUri, null);
    }

    public static void deleteAlbumArt(@NonNull Context context, long albumId) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri localUri = Uri.parse("content://media/external/audio/albumart");
        contentResolver.delete(ContentUris.withAppendedId(localUri, albumId), null, null);
        contentResolver.notifyChange(localUri, null);
    }

    @NonNull
    public static File createAlbumArtFile() {
        return new File(createAlbumArtDir(), String.valueOf(System.currentTimeMillis()));
    }

    @NonNull
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File createAlbumArtDir() {
        File albumArtDir = new File(Environment.getExternalStorageDirectory(), "/albumthumbs/");
        if (!albumArtDir.exists()) {
            albumArtDir.mkdirs();
            try {
                new File(albumArtDir, ".nomedia").createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return albumArtDir;
    }

    public static boolean isArtistNameUnknown(@Nullable String artistName) {
        if (TextUtils.isEmpty(artistName)) return false;
        if (artistName.equals(Artist.UNKNOWN_ARTIST_DISPLAY_NAME)) return true;
        artistName = artistName.trim().toLowerCase();
        return artistName.equals("unknown") || artistName.equals("<unknown>");
    }

    @NonNull
    public static String getSectionName(@Nullable String musicMediaTitle) {
        if (TextUtils.isEmpty(musicMediaTitle)) return "";
        musicMediaTitle = musicMediaTitle.trim().toLowerCase();
        if (musicMediaTitle.startsWith("the ")) {
            musicMediaTitle = musicMediaTitle.substring(4);
        } else if (musicMediaTitle.startsWith("a ")) {
            musicMediaTitle = musicMediaTitle.substring(2);
        }
        if (musicMediaTitle.isEmpty()) return "";
        return String.valueOf(musicMediaTitle.charAt(0)).toUpperCase();
    }

    /**
     * convert a timestamp to a readable String
     *
     * @param t timeStamp to parse (Unit: milliseconds)
     * @return human-friendly time
     */
    @SuppressLint("DefaultLocale")
    public static String parseTimeStamp(int t) {
        long ms = t % 1000;
        long s = (t % (1000 * 60)) / 1000;
        long m = (t - s * 1000 - ms) / (1000 * 60);
        return String.format("%d:%02d.%03d", m, s, ms);
//        return m + ":" + s + "." + ms;
    }

    @NonNull
    public static ArrayList<Song> getAlbumSongList(List<Album> albums) {
        ArrayList<Song> songs = new ArrayList<>(1);
        for (Album album : albums) {
            songs.addAll(album.songs);
        }
        return songs;
    }

    @NonNull
    public static ArrayList<Song> getArtistSongList(List<Artist> artists) {
        ArrayList<Song> songs = new ArrayList<>(1);
        for (Artist artist : artists) {
            songs.addAll(artist.getSongs());
        }
        return songs;
    }

    @NonNull
    public static ArrayList<Song> getGenreSongList(List<Genre> genres) {
        ArrayList<Song> songs = new ArrayList<>(1);
        for (Genre genre : genres) {
            songs.addAll(GenreLoader.INSTANCE.getSongs(App.getInstance(), genre.id));
        }
        return songs;
    }

}
