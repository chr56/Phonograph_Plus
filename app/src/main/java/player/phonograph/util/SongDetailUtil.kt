/*
 * Copyright (c) 2022 chr_56
 */

@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package player.phonograph.util

import mt.pref.ThemeColor
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
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.model.LongFilePropertyField
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.model.StringFilePropertyField
import player.phonograph.model.TagField
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.IOException

object SongDetailUtil {
    fun getFileSizeString(sizeInBytes: Long): String {
        val fileSizeInKB: Long = sizeInBytes / 1024
        val fileSizeInMB: Long = fileSizeInKB / 1024
        val fileSizeInMBf: Float = fileSizeInKB / 1024F

        val readableFileSizeInMB =
            fileSizeInMB.toString() +
                    ((fileSizeInMBf - fileSizeInMB).toString()).let {
                        if (it.isNotBlank() && it.length >= 5) it.substring(1, 4) else ".0"
                    }

        return "$readableFileSizeInMB MB ($fileSizeInKB KB)"
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
                songInfo.bitRate = audioHeader.bitRate + " kb/s"
                songInfo.samplingRate = audioHeader.sampleRate + " Hz"
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
                        Toast.makeText(App.instance,
                            "Other tags in this song is too many, only show the first 32 entries",
                            Toast.LENGTH_LONG)
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

    fun readSong(songFile: File): SongInfoModel {
        require(songFile.exists()) { "${songFile.path} doesn't exist! please check file or permission of device storage." }
        val fileName = songFile.name
        val filePath = songFile.absolutePath
        val fileSize = songFile.length()
        try {
            val audioFile: AudioFile = AudioFileIO.read(songFile)
            // files of the song
            val audioHeader: AudioHeader = audioFile.audioHeader
            val fileFormat = audioHeader.format
            val trackLength = (audioHeader.trackLength * 1000).toLong()
            val bitRate = "${audioHeader.bitRate} kb/s"
            val samplingRate = "${audioHeader.sampleRate} Hz"
            // tags of the song
            val title = readTagField(audioFile, FieldKey.TITLE)
            val artist = readTagField(audioFile, FieldKey.ARTIST)
            val album = readTagField(audioFile, FieldKey.ALBUM)
            val albumArtist = readTagField(audioFile, FieldKey.ALBUM_ARTIST)
            val composer = readTagField(audioFile, FieldKey.COMPOSER)
            val lyricist = readTagField(audioFile, FieldKey.LYRICIST)
            val year = readTagField(audioFile, FieldKey.YEAR)
            val genre = readTagField(audioFile, FieldKey.GENRE)
            val track = readTagField(audioFile, FieldKey.TRACK)
            val comment = readTagField(audioFile, FieldKey.COMMENT)
            // tags of custom field
            val otherTags = readCustomTags(audioFile)
            return SongInfoModel(
                fileName = StringFilePropertyField(fileName),
                filePath = StringFilePropertyField(filePath),
                fileFormat = StringFilePropertyField(fileFormat),
                bitRate = StringFilePropertyField(bitRate),
                samplingRate = StringFilePropertyField(samplingRate),
                fileSize = LongFilePropertyField(fileSize),
                trackLength = LongFilePropertyField(trackLength),
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                composer = composer,
                lyricist = lyricist,
                year = year,
                genre = genre,
                track = track,
                comment = comment,
                otherTags
            )
        } catch (e: Exception) {
            Log.e("TagRead", "error while reading the song file", e)
            return SongInfoModel.EMPTY().also {
                it.fileName = StringFilePropertyField(fileName)
                it.filePath = StringFilePropertyField(filePath)
                it.fileSize = LongFilePropertyField(fileSize)
                it.title = TagField(FieldKey.KEY, "error while reading the song file")
            }
        }
    }

    private fun readTagField(audioFile: AudioFile, id: FieldKey): TagField =
        TagField(id, audioFile.tag.getFirst(id))

    private fun readCustomTags(audioFile: AudioFile): MutableMap<String, String>? {
        val customInfoField = audioFile.tag.getFields("TXXX")
        val otherTags = if (customInfoField != null && customInfoField.size > 0) {
            if (customInfoField.size >= 32) {
                Toast.makeText(
                    App.instance,
                    "Other tags in this song is too many, only show the first 32 entries",
                    Toast.LENGTH_LONG
                ).show()
            }

            val limit = if (customInfoField.size <= 32) customInfoField.size else 31
            val customTags: MutableMap<String, String> = HashMap()
            for (index in 0 until limit) {
                val field = customInfoField[index] as AbstractTagFrame
                customTags.put(
                    field.body.getObjectValue(DataTypes.OBJ_DESCRIPTION) as String,
                    field.body.getObjectValue(DataTypes.OBJ_TEXT) as String
                )
            }
            customTags
        } else null
        return otherTags
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

    fun loadArtwork(context: Context, song: Song, onLoaded: () -> Unit): MutableState<BitmapPaletteWrapper?> {
        val bitmapState = mutableStateOf<BitmapPaletteWrapper?>(
            BitmapPaletteWrapper(ContextCompat.getDrawable(context, R.drawable.default_album_art)!!
                .toBitmap(),
                ThemeColor.primaryColor(context))
        )
        loadImage(context) {
            data(song)
            target(PaletteTargetBuilder(context)
                .onResourceReady { result: Drawable, paletteColor: Int ->
                    bitmapState.value =
                        BitmapPaletteWrapper(result.toBitmap(), paletteColor)

                    onLoaded.invoke()
                }
                .build())
        }
        return bitmapState
    }

    class BitmapPaletteWrapper(var bitmap: Bitmap, var paletteColor: Int)
}
