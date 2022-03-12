/*
 * Copyright (c) 2022 chr_56
 */

@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package player.phonograph.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.tag.datatype.DataTypes
import org.jaudiotagger.tag.id3.AbstractTagFrame
import player.phonograph.App
import player.phonograph.glide.SongGlideRequest
import player.phonograph.glide.palette.BitmapPaletteWrapper
import player.phonograph.model.Song
import java.io.File
import java.io.IOException

object SongDetailUtil {
    fun getFileSizeString(sizeInBytes: Long): String {
        val fileSizeInKB = sizeInBytes / 1024
        val fileSizeInMB = fileSizeInKB / 1024
        return "$fileSizeInMB MB"
    }
    fun loadSong(song: Song): SongInfo {
        val file = File(song.data)
        return if (file.exists())
            loadSong(file)
        else
            SongInfo("FILE NOT FOUND")
    }
    fun loadSong(songFile: File): SongInfo {
        val songInfo = SongInfo()

        if (songFile.exists()) {
            songInfo.fileName = songFile.name
            songInfo.filePath = songFile.absolutePath
            songInfo.fileSize = songFile.length()
            try {
                val audioFile: AudioFile = AudioFileIO.read(songFile)
                // files of the song
                val audioHeader: AudioHeader = audioFile.audioHeader
                songInfo.fileFormat = audioHeader.format
                songInfo.trackLength = (audioHeader.trackLength * 1000).toLong()
                songInfo.bitRate = audioHeader.bitRate // + " kb/s"
                songInfo.samplingRate = audioHeader.sampleRate // + " Hz"
                // tags of the song
                songInfo.title = audioFile.tag.getFirst(FieldKey.TITLE)
                songInfo.artist = audioFile.tag.getFirst(FieldKey.ARTIST)
                songInfo.album = audioFile.tag.getFirst(FieldKey.ALBUM)
                songInfo.albumArtist = audioFile.tag.getFirst(FieldKey.ALBUM_ARTIST)
                songInfo.composer = audioFile.tag.getFirst(FieldKey.COMPOSER)
                songInfo.lyricist = audioFile.tag.getFirst(FieldKey.LYRICIST)
                songInfo.year = audioFile.tag.getFirst(FieldKey.YEAR)
                songInfo.genre = audioFile.tag.getFirst(FieldKey.GENRE)
                songInfo.track = audioFile.tag.getFirst(FieldKey.TRACK)
                songInfo.comment = audioFile.tag.getFirst(FieldKey.COMMENT)
                // tags of custom field
                val customTags: MutableMap<String, String> = HashMap()
                val customInfoField = audioFile.tag.getFields("TXXX")
                if (customInfoField != null && customInfoField.size > 0) {
                    if (customInfoField.size >= 32) {
                        Toast.makeText(App.instance, "Other tags in this song is too many, only show the first 32 entries", Toast.LENGTH_LONG)
                            .show()
                    }

                    val limit = if (customInfoField.size <= 32) customInfoField.size else 31
                    for (index in 0 until limit) {
                        val field = customInfoField[index] as AbstractTagFrame
                        customTags.put(
                            field.body.getObjectValue(DataTypes.OBJ_DESCRIPTION) as String,
                            field.body.getObjectValue(DataTypes.OBJ_TEXT) as String
                        )
                    }
                    songInfo.otherTags = customTags
                }
            } catch (e: Exception) {
                when (e) {
                    is CannotReadException, is TagException, is ReadOnlyFileException, is InvalidAudioFrameException, is IOException -> {
                        Log.e("TagRead", "error while reading the song file", e)
                        return songInfo.apply { title = "error while reading the song file" }
                    }
                    else -> throw e
                }
            }
        }
        return songInfo
    }

    class SongInfo(
        var fileName: String? = "-",
        var filePath: String? = "-",
        var fileFormat: String? = "-",
        var bitRate: String? = "-",
        var samplingRate: String? = "-",
        var fileSize: Long? = 0,
        var trackLength: Long? = 0,
        var title: String? = "-",
        var artist: String? = "-",
        var album: String? = "-",
        var albumArtist: String? = "-",
        var composer: String? = "-",
        var lyricist: String? = "-",
        var year: String? = "0",
        var genre: String? = "-",
        var track: String? = "-",
        var comment: String? = "",
        var otherTags: MutableMap<String, String>? = null,
    )

    fun loadArtwork(context: Context, song: Song, callback: () -> Unit): MutableState<BitmapPaletteWrapper?> {
        val bitmapState = mutableStateOf<BitmapPaletteWrapper?>(null)
        getRequestBuilder(context, song)
            .into(object : CustomTarget<BitmapPaletteWrapper?>() {
                override fun onResourceReady(resource: BitmapPaletteWrapper, transition: Transition<in BitmapPaletteWrapper?>?) {
                    bitmapState.value = resource
                    callback.invoke()
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    val bitmap = placeholder?.toBitmap()
                    bitmap?.let {
                        bitmapState.value = BitmapPaletteWrapper(bitmap, Palette.from(bitmap).generate())
                    }
                }
            })
        return bitmapState
    }

    private fun getRequestBuilder(context: Context, song: Song): RequestBuilder<BitmapPaletteWrapper> {
        return SongGlideRequest.Builder.from(Glide.with(context), song)
            .checkIgnoreMediaStore(context).generatePalette(context).build()
    }
}
