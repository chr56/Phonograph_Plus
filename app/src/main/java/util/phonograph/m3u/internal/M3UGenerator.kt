/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package util.phonograph.m3u.internal

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import player.phonograph.App
import player.phonograph.R
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.util.Util
import java.io.*
import java.lang.StringBuilder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

object M3UGenerator {

    @JvmStatic
    @Deprecated("don not use File")
    @Throws(IOException::class)
    fun writeFile(context: Context?, dir: File, playlist: Playlist): File {
        if (!dir.exists()) dir.mkdirs() //noinspection ResultOfMethodCallIgnored

        val songs: List<Song> = playlist.getSongs(context ?: App.instance)

        val filename: String = playlist.name +
            if (playlist is SmartPlaylist) {
                // Since AbsCustomPlaylists are dynamic, we add a timestamp after their names.
                SimpleDateFormat("_yy-MM-dd_HH-mm", Locale.getDefault()).format(Util.currentDate())
            } else ""

        val file = File(dir, "$filename.$EXTENSION")
        if (songs.isNotEmpty()) {
            val bw = BufferedWriter(FileWriter(file))

            bw.write(HEADER)
            for (song in songs) {
                bw.write(
                    createLine(song)
                )
            }

            bw.close()
        }

        return file
    }

    @JvmStatic
    fun generate(outputStream: OutputStream, context: Context?, playlist: Playlist, addHeader: Boolean) {

        val songs: List<Song> =
            if (playlist is SmartPlaylist) {
                playlist.getSongs(context ?: App.instance)
            } else {
                PlaylistSongLoader.getPlaylistSongList(context ?: App.instance, playlist.id)
            }

        if (songs.isNotEmpty()) {
            GlobalScope.launch(context = Dispatchers.IO) {
                try {
                    generate(outputStream, songs, addHeader)
                } catch (e: IOException) {
                    Util.coroutineToast(context ?: App.instance, R.string.failed)
                }
            }
        }
        return
    }

    @JvmStatic
    @Throws(IOException::class)
    fun generate(outputStream: OutputStream, songs: List<Song>, addHeader: Boolean) {
        if (songs.isNotEmpty()) {
            val w = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
            if (addHeader) w.write(HEADER)
            for (song in songs) {
                w.write(
                    createLine(song)
                )
            }
            w.close()
        }
    }

    private fun createLine(song: Song): String {
        val b = StringBuilder("\n")
        b.appendLine()
        b.append("$ENTRY${song.duration}$DURATION_SEPARATOR${song.artistName} - ${song.title}")
        b.appendLine()
        b.append(song.data)
        return b.toString()
    }

    private const val EXTENSION = "m3u"
    private const val HEADER = "#EXTM3U"
    private const val ENTRY = "#EXTINF:"
    private const val DURATION_SEPARATOR = ","
}
