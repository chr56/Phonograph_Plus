/*
 *  Copyright (c) 2022~2023 chr_56
 */

@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package player.phonograph.mechanism

import lib.phonograph.misc.ICreateFileStorageAccess
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.ID3v22Frames
import org.jaudiotagger.tag.id3.ID3v22Tag
import org.jaudiotagger.tag.id3.ID3v23Frames
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.id3.ID3v24Frames
import org.jaudiotagger.tag.id3.ID3v24Tag
import player.phonograph.coil.loadImage
import player.phonograph.coil.retriever.PARAMETERS_RAW
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.mechanism.tageditor.TagFormat
import player.phonograph.model.LongFilePropertyField
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.model.StringFilePropertyField
import player.phonograph.model.TagField
import player.phonograph.util.reportError
import player.phonograph.util.warning
import androidx.core.graphics.drawable.toBitmap
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.OutputStream

object SongDetail {

    fun readSong(song: Song): SongInfoModel = readSong(File(song.data))

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
            val tagFormat = TagFormat.of(audioFile.tag)
            val allTags = readAllTags(audioFile)
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
                tagFormat = tagFormat,
                allTags,
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

    private fun readAllTags(audioFile: AudioFile): Map<String, String> {
        val items: Map<String, String> =
            when (val tag = audioFile.tag) {
                is AbstractID3v2Tag -> readID3v2Tags(tag)
                else                -> emptyMap()
            }
        return items
    }

    private const val ERR_PARSE_FIELD = "<Err: failed to read field>"
    private const val ERR_PARSE_KEY = "<Err: failed to process key>"

    fun readID3v2Tags(tag: AbstractID3v2Tag): Map<String, String> {
        return tag.frameMap
            .mapValues { (key, frame) ->
                frame as org.jaudiotagger.tag.TagField
                if (frame.isBinary) {
                    "<Binary Data>"
                } else if (frame.isEmpty) {
                    "<Empty>"
                } else {
                    try {
                        val id3v2Frame = frame as AbstractID3v2Frame
                        val frameBody = id3v2Frame.body
                        frameBody.userFriendlyValue
                    } catch (e: Exception) {
                        reportError(e, "readID3v2Tags", ERR_PARSE_FIELD)
                        ERR_PARSE_FIELD
                    }
                }
            }
            .mapKeys { (key, frame) ->
                val frames = when (tag) {
                    is ID3v24Tag -> ID3v24Frames.getInstanceOf()
                    is ID3v23Tag -> ID3v23Frames.getInstanceOf()
                    is ID3v22Tag -> ID3v22Frames.getInstanceOf()
                    else         -> null
                }
                if (frames != null) {
                    val description = frames.idToValueMap.getOrDefault(key, ERR_PARSE_KEY)
                    "[$key]$description"
                } else {
                    key
                }
            }
    }

    fun loadArtwork(
        context: Context,
        container: MutableStateFlow<BitmapPaletteWrapper?>,
        data: Any,
    ) {
        loadImage(context) {
            data(data)
            parameters(PARAMETERS_RAW)
            target(
                PaletteTargetBuilder(context)
                    .onResourceReady { result: Drawable, paletteColor: Int ->
                        val success =
                            container.tryEmit(
                                BitmapPaletteWrapper(result.toBitmap(), paletteColor)
                            )
                        if (!success) warning("LoadArtwork", "Failed to load artwork!")
                    }
                    .build()
            )
        }
    }

    fun saveArtwork(
        coroutineScope: CoroutineScope,
        activity: Context,
        wrapper: BitmapPaletteWrapper,
        fileName: String,
    ) {
        if (activity is ICreateFileStorageAccess) {
            val accessTool = activity.createFileStorageAccessTool
            accessTool.launch("$fileName.jpg") { uri ->
                if (uri != null) {
                    saveArtworkImpl(coroutineScope, activity, uri, wrapper)
                } else {
                    warning("SaveArtWorkImpl", "Failed to create File")
                }
            }
        } else {
            throw IllegalStateException("${activity.javaClass} can not create file!")
        }
    }


    private fun saveArtworkImpl(
        coroutineScope: CoroutineScope,
        context: Context,
        uri: Uri,
        wrapper: BitmapPaletteWrapper,
    ) {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("can't open uri $uri")
        writeArtwork(coroutineScope, stream, wrapper)
    }

    private fun writeArtwork(
        coroutineScope: CoroutineScope,
        outputStream: OutputStream,
        wrapper: BitmapPaletteWrapper,
    ) {
        // write
        coroutineScope.launch(Dispatchers.IO) {
            outputStream.buffered(4096).use { outputStream ->
                wrapper.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }
    }

    class BitmapPaletteWrapper(var bitmap: Bitmap, var paletteColor: Int)
}
