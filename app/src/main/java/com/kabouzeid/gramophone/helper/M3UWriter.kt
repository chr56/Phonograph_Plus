package com.kabouzeid.gramophone.helper

import android.content.Context
import com.kabouzeid.gramophone.loader.PlaylistSongLoader
import com.kabouzeid.gramophone.model.AbsCustomPlaylist
import com.kabouzeid.gramophone.model.Playlist
import com.kabouzeid.gramophone.model.Song
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object M3UWriter {
    private const val EXTENSION = "m3u"
    private const val HEADER = "#EXTM3U"
    private const val ENTRY = "#EXTINF:"
    private const val DURATION_SEPARATOR = ","

    @JvmStatic
    @Throws(IOException::class)
    fun write(context: Context?, dir: File, playlist: Playlist): File {
        if (!dir.exists()) dir.mkdirs() //noinspection ResultOfMethodCallIgnored
        val filename: String

        val songs: List<Song>
        if (playlist is AbsCustomPlaylist) {
            songs = playlist.getSongs(context)

            // Since AbsCustomPlaylists are dynamic, we add a timestamp after their names.
            filename =
                playlist.name + SimpleDateFormat("_yy-MM-dd_HH-mm", Locale.getDefault()).format(
                Calendar.getInstance().time
            )
        } else {
            songs = PlaylistSongLoader.getPlaylistSongList(context!!, playlist.id)
            filename = playlist.name
        }

        val file = File(dir, "$filename.$EXTENSION")
        if (songs.isNotEmpty()) {
            val bw = BufferedWriter(FileWriter(file))

            bw.write(HEADER)
            for (song in songs) {
                bw.newLine()
                bw.write(ENTRY + song.duration + DURATION_SEPARATOR + song.artistName + " - " + song.title)
                bw.newLine()
                bw.write(song.data)
            }

            bw.close()
        }

        return file
    }
}
