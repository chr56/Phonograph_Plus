/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.lyrics

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.logging.ErrorMessage
import org.jaudiotagger.tag.FieldKey
import player.phonograph.App
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.lyrics.LyricsInfo
import player.phonograph.model.lyrics.LyricsSource
import player.phonograph.model.lyrics.TextLyrics
import player.phonograph.util.debug
import player.phonograph.util.file.stripExtension
import player.phonograph.util.permissions.StoragePermissionChecker
import android.content.Context
import android.net.Uri
import android.util.Log
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.File

object LyricsLoader {

    private val backgroundCoroutine: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    suspend fun loadLyrics(songFile: File, songTitle: String): LyricsInfo? {

        if (!StoragePermissionChecker.hasStorageReadPermission(App.instance)) {
            debug {
                Log.v(TAG, "No storage read permission to fetch lyrics for $songTitle")
            }
            return null
        }

        // embedded
        val embedded = backgroundCoroutine.async(Dispatchers.IO) {
            parseEmbedded(songFile, LyricsSource.Embedded)
        }

        // external
        val externalPrecise = backgroundCoroutine.async(Dispatchers.IO) {
            val files = getExternalPreciseLyricsFile(songFile)
            files.mapNotNull { parseExternal(it, LyricsSource.ExternalPrecise) }
        }
        val external = backgroundCoroutine.async(Dispatchers.IO) {
            val files = searchExternalVagueLyricsFiles(songFile, songTitle)
            files.mapNotNull { parseExternal(it, LyricsSource.ExternalDecorated) }
        }

        // collect
        val all = buildList {
            val embeddedLyrics = embedded.await()
            if (embeddedLyrics != null) add(embeddedLyrics)
            val preciseLyrics = externalPrecise.await()
            addAll(preciseLyrics)
            val vagueLyrics = external.await()
            addAll(vagueLyrics)
        }


        val activated: Int = all.indexOfFirst { it is LrcLyrics }

        // end of fetching
        return LyricsInfo(all, activated)
    }

    private fun parseEmbedded(
        songFile: File,
        lyricsSource: LyricsSource = LyricsSource.Embedded,
    ): AbsLyrics? = tryLoad(songFile) {
        AudioFileIO.read(songFile).tag?.getFirst(FieldKey.LYRICS).let { str ->
            if (str != null && str.trim().isNotBlank()) {
                parse(str, lyricsSource)
            } else {
                null
            }
        }
    }

    private fun parseExternal(
        file: File,
        lyricsSource: LyricsSource = LyricsSource.Unknown,
    ): AbsLyrics? = tryLoad(file) {
        if (file.exists()) {
            val content = file.readText()
            if (content.isNotEmpty()) parse(content, lyricsSource) else null
        } else {
            null
        }
    }

    private fun tryLoad(songFile: File, block: () -> AbsLyrics?): AbsLyrics? =
        try {
            block()
        } catch (e: CannotReadException) {
            val errorMsg = errorMsg(songFile.path, e)
            val suffix = songFile.name.substringAfterLast('.', "")
            if (ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(suffix) == e.message) {
                // ignore
            } else {
                Log.i(TAG, errorMsg)
            }
            TextLyrics.from(errorMsg)
        } catch (e: Exception) {
            val errorMsg = errorMsg(songFile.path, e)
            Log.i(TAG, errorMsg)
            TextLyrics.from(errorMsg)
        }

    private fun errorMsg(path: String, t: Throwable?) =
        "$ERR_MSG_HEADER $path: ${t?.message}\n${Log.getStackTraceString(t)}"


    private fun parse(raw: String, lyricsSource: LyricsSource = LyricsSource.Unknown): AbsLyrics {
        val rawLength = raw.length
        val sampleLineCount = max(rawLength / 100, 10)
        val sampleLength = max(min(rawLength / 8, 480), 60)

        val lines = raw.take(sampleLength).splitToSequence("\r\n", "\n", "\r", limit = sampleLineCount)
        val regex = Regex("""(\[.+\])+\s*.*""")

        var score = 0
        for (line in lines) score += if (regex.matches(line)) 1 else -1

        return if (score > 1) LrcLyrics.from(raw, lyricsSource) else TextLyrics.from(raw, lyricsSource)
    }

    fun getExternalPreciseLyricsFile(songFile: File): List<File> {
        val filename = stripExtension(songFile.absolutePath)
        val lrc = File("$filename.lrc").takeIf { it.exists() }
        val txt = File("$filename.txt").takeIf { it.exists() }
        return listOfNotNull(lrc, txt)
    }

    fun searchExternalVagueLyricsFiles(songFile: File, songTitle: String): List<File> {
        val dir = songFile.absoluteFile.parentFile ?: return emptyList()

        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val fileName = stripExtension(songFile.name)
        val eFileName = Regex.escape(fileName)
        val eSongName = Regex.escape(songTitle)

        // vague pattern
        val vagueRegex =
            Regex(""".*[-;]?($eFileName|$eSongName)[-;]?.*\.(lrc|txt)""", RegexOption.IGNORE_CASE)

        // start list file under the same dir
        val files = try {
            val list = dir.list() ?: return emptyList()
            list.filter { name ->
                when {
                    "$fileName.lrc" == name  -> false
                    "$fileName.txt" == name  -> false
                    vagueRegex.matches(name) -> true
                    else                     -> false
                }
            }.map { File(dir, it) }
        } catch (e: Exception) {
            Log.v(TAG, "Failed to list files: ${e.message}")
            emptyList()
        }

        return if (files.isNotEmpty()) {
            debug {
                Log.v(TAG, files.fold("All lyrics found:") { acc, str -> "${str.path};$acc" })
            }
            files
        } else {
            emptyList()
        }
    }


    fun parseFromUri(context: Context, uri: Uri): AbsLyrics? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.reader().use {
                parse(it.readText(), LyricsSource.ManuallyLoaded)
            }
        }
    }


    private const val TAG = "LyricsLoader"
    private const val ERR_MSG_HEADER = "Failed to read "
}
