/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.migrate

import okio.BufferedSink
import player.phonograph.MusicServiceMsgConst
import player.phonograph.mediastore.searchSong
import player.phonograph.model.Song
import player.phonograph.provider.FavoriteSongsStore
import player.phonograph.provider.MusicPlaybackQueueStore
import player.phonograph.provider.PathFilterStore
import player.phonograph.util.FileUtil.saveToFile
import player.phonograph.util.reportError
import player.phonograph.util.warning
import androidx.annotation.Keep
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlin.LazyThreadSafetyMode.NONE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object DatabaseBackupManger {

    const val VERSION = "version"
    const val VERSION_CODE = 0


    private val parser by lazy(NONE) {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    private const val WHITE_LIST = "whitelist"
    private const val BLACK_LIST = "blacklist"

    /**
     * @param destinationUri destination document uri
     */
    fun exportPathFilter(context: Context, destinationUri: Uri): Boolean {
        val json = exportPathFilter(context)
        val content = parser.encodeToString(json)
        saveToFile(destinationUri, content, context.contentResolver)
        return true
    }

    fun exportPathFilter(sink: BufferedSink, context: Context): Boolean {
        return try {
            val json = exportPathFilter(context)
            val content = parser.encodeToString(json)
            sink.writeString(content, Charsets.UTF_8)
            true
        } catch (e: IOException) {
            reportError(e, TAG, "Failed to export PathFilter!")
            false
        }
    }

    /**
     * close stream after use
     */
    fun exportPathFilter(context: Context, outputStream: OutputStream) {
        val json = exportPathFilter(context)
        val content = parser.encodeToString(json)
        outputStream.writer(Charsets.UTF_8).also {
            it.write(content)
            it.flush()
        }
    }

    private fun exportPathFilter(context: Context): JsonObject {
        val db = PathFilterStore.getInstance(context)
        val wl = db.whitelistPaths.map { JsonPrimitive(it) }
        val bl = db.blacklistPaths.map { JsonPrimitive(it) }
        return JsonObject(
            mapOf(
                VERSION to JsonPrimitive(VERSION_CODE),
                WHITE_LIST to JsonArray(wl),
                BLACK_LIST to JsonArray(bl),
            )
        )
    }

    fun importPathFilter(context: Context, inputStream: InputStream): Boolean {
        val rawString = inputStream.reader().use { it.readText() }
        return importPathFilter(context, rawString)
    }

    fun importPathFilter(context: Context, sourceUri: Uri): Boolean {
        val rawString: String = readFrom(context, sourceUri)
        return importPathFilter(context, rawString)
    }

    private fun importPathFilter(context: Context, rawString: String): Boolean {
        val json = parser.parseToJsonElement(rawString) as? JsonObject
        if (json != null) {
            try {
                importPathFilter(context, json, true)
            } catch (e: Exception) {
                reportError(e, TAG, "Failed!")
            }
        } else {
            warning(TAG, "Nothing to import")
        }
        return true
    }

    private fun importPathFilter(context: Context, json: JsonObject, override: Boolean) {

        val wl = json[WHITE_LIST] as? JsonArray
        val bl = json[BLACK_LIST] as? JsonArray


        val db = PathFilterStore.getInstance(context)

        if (!(wl == null && bl == null)) {

            bl?.map { (it as JsonPrimitive).content }?.let {
                if (override) db.clearBlacklist()
                db.addBlacklistPath(it)
            }

            wl?.map { (it as JsonPrimitive).content }?.let {
                if (override) db.clearWhitelist()
                db.addWhitelistPath(it)
            }

        } else {
            warning(TAG, "Nothing to import")
        }

    }

    private const val PLAYING_QUEUE = "playing_queue"
    private const val ORIGINAL_PLAYING_QUEUE = "original_playing_queue"

    /**
     * @param destinationUri destination document uri
     */
    fun exportPlayingQueues(context: Context, destinationUri: Uri): Boolean {
        val json = exportPlayingQueues(context)
        val content = parser.encodeToString(json)
        saveToFile(destinationUri, content, context.contentResolver)
        return true
    }



    fun exportPlayingQueues(sink: BufferedSink, context: Context): Boolean {
        return try {
            val json = exportPlayingQueues(context)
            val content = parser.encodeToString(json)
            sink.writeString(content, Charsets.UTF_8)
            true
        } catch (e: IOException) {
            reportError(e, TAG, "Failed to export PlayingQueues!")
            false
        }
    }
    /**
     * close stream after use
     */
    fun exportPlayingQueues(context: Context, outputStream: OutputStream) {
        val json = exportPlayingQueues(context)
        val content = parser.encodeToString(json)
        outputStream.writer(Charsets.UTF_8).also {
            it.write(content)
            it.flush()
        }
    }

    private fun exportPlayingQueues(context: Context): JsonObject {
        val db = MusicPlaybackQueueStore.getInstance(context)
        val oq = db.savedOriginalPlayingQueue.map(::persistentSong)
        val pq = db.savedPlayingQueue.map(::persistentSong)
        return JsonObject(
            mapOf(
                VERSION to JsonPrimitive(VERSION_CODE),
                PLAYING_QUEUE to JsonArray(pq),
                ORIGINAL_PLAYING_QUEUE to JsonArray(oq),
            )
        )
    }

    fun importPlayingQueues(context: Context, sourceUri: Uri): Boolean {
        val rawString: String = readFrom(context, sourceUri)
        return importPlayingQueues(context, rawString)
    }

    fun importPlayingQueues(context: Context, inputStream: InputStream): Boolean {
        val rawString = inputStream.reader().use { it.readText() }
        return importPlayingQueues(context, rawString)
    }

    private fun importPlayingQueues(context: Context, rawString: String): Boolean {
        val json = parser.parseToJsonElement(rawString) as? JsonObject
        if (json != null) {
            try {
                importPlayingQueues(context, json)
            } catch (e: Exception) {
                reportError(e, TAG, "Failed!")
            }
        } else {
            warning(TAG, "Nothing to import")
        }
        return true
    }


    private fun importPlayingQueues(context: Context, json: JsonObject) {
        val oq = json[ORIGINAL_PLAYING_QUEUE] as? JsonArray
        val pq = json[PLAYING_QUEUE] as? JsonArray


        val db = MusicPlaybackQueueStore.getInstance(context)

        val originalQueue = recoverSongs(context, oq)
        val currentQueueQueue = recoverSongs(context, pq)

        if (!(originalQueue == null && currentQueueQueue == null)) {

            // todo: report imported queues

            db.saveQueues(
                currentQueueQueue ?: originalQueue ?: emptyList(),
                originalQueue ?: currentQueueQueue ?: emptyList(),
            )
            context.sendBroadcast(Intent(MusicServiceMsgConst.MEDIA_STORE_CHANGED))

        } else {
            warning(TAG, "Nothing to import")
        }
    }

    private const val FAVORITE = "favorite"

    /**
     * @param destinationUri destination document uri
     */
    fun exportFavorites(context: Context, destinationUri: Uri): Boolean {
        val json = exportFavorites(context)
        val content = parser.encodeToString(json)
        saveToFile(destinationUri, content, context.contentResolver)
        return true
    }


    fun exportFavorites(sink: BufferedSink, context: Context): Boolean {
        return try {
            val json = exportFavorites(context)
            val content = parser.encodeToString(json)
            sink.writeString(content, Charsets.UTF_8)
            true
        } catch (e: IOException) {
            reportError(e, TAG, "Failed to export Favorites!")
            false
        }
    }

    /**
     * close stream after use
     */
    fun exportFavorites(context: Context, outputStream: OutputStream) {
        val json = exportFavorites(context)
        val content = parser.encodeToString(json)
        outputStream.writer(Charsets.UTF_8).also {
            it.write(content)
            it.flush()
        }
    }

    private fun exportFavorites(context: Context): JsonObject {
        val db = FavoriteSongsStore.instance

        val songs = db.getAllSongs(context).map(::persistentSong)
        return JsonObject(
            mapOf(
                VERSION to JsonPrimitive(VERSION_CODE),
                FAVORITE to JsonArray(songs)
            )
        )
    }

    fun importFavorites(context: Context, inputStream: InputStream): Boolean {
        val rawString = inputStream.reader().use { it.readText() }
        return importFavorites(context, rawString)
    }

    fun importFavorites(context: Context, sourceUri: Uri): Boolean {
        val rawString: String = readFrom(context, sourceUri)
        return importFavorites(context, rawString)
    }

    fun importFavorites(context: Context, rawString: String): Boolean {
        val json = parser.parseToJsonElement(rawString) as? JsonObject
        if (json != null) {
            try {
                importFavorites(context, json, true)
            } catch (e: Exception) {
                reportError(e, TAG, "Failed!")
            }
        } else {
            warning(TAG, "Nothing to import")
        }
        return true
    }

    private fun importFavorites(context: Context, json: JsonObject, override: Boolean) {
        val f = json[FAVORITE] as? JsonArray

        val db = FavoriteSongsStore.instance

        val songs = recoverSongs(context, f)

        if (!songs.isNullOrEmpty()) {

            // todo: report imported songs
            if (override) db.clear()
            db.addAll(songs.asReversed())
            context.sendBroadcast(Intent(MusicServiceMsgConst.MEDIA_STORE_CHANGED))
        } else {
            warning(TAG, "Nothing to import")
        }
    }

    private fun persistentSong(song: Song): JsonElement =
        parser.encodeToJsonElement(PersistentSong.serializer(), PersistentSong.from(song))

    private fun recoverSongs(context: Context, array: JsonArray?): List<Song>? =
        array?.map { parser.decodeFromJsonElement(PersistentSong.serializer(), it) }
            ?.mapNotNull { it.getMatchingSong(context) }


    @Keep
    @Serializable
    class PersistentSong(
        @SerialName("path") val path: String,
        @SerialName("title") val title: String,
        @SerialName("album") val album: String?,
        @SerialName("artist") val artist: String?,
    ) {
        companion object {
            fun from(song: Song): PersistentSong =
                PersistentSong(song.data, song.title, song.albumName, song.artistName)
        }

        fun getMatchingSong(context: Context): Song? {
            val song = searchSong(context, path)
            if (song == Song.EMPTY_SONG) return null
            return song
        }
    }

    /**
     * @param uri source document content uri
     */
    private fun readFrom(context: Context, uri: Uri): String {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                FileInputStream(it.fileDescriptor).use { fileInputStream ->
                    fileInputStream.use { stream ->
                        return stream.bufferedReader().use { reader -> reader.readText() }
                    }
                }
            }
        } catch (e: Exception) {
            reportError(e, TAG, "Could not read content from $uri")
        }
        return ""
    }

    private const val TAG = "DatabaseBackupManger"

}