/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.misc

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.logging.ErrorMessage
import org.jaudiotagger.tag.FieldKey
import player.phonograph.App
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.model.Song
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.lyrics.LyricsList
import player.phonograph.model.lyrics.LyricsSource
import player.phonograph.model.lyrics.TextLyrics
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting
import player.phonograph.util.FileUtil
import player.phonograph.util.Util.debug
import java.io.File

object LyricsLoader {

    private val backgroundCoroutine: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    suspend fun loadLyrics(songFile: File, song: Song): LyricsList {
        if (!Setting.instance.enableLyrics) {
            debug {
                Log.v(TAG,"Lyrics is off for ${song.title}")
            }
            return LyricsList()
        }

        // embedded
        val embedded = backgroundCoroutine.async(Dispatchers.IO) {
            parseEmbedded(songFile, LyricsSource.Embedded())
        }

        // external
        val external = backgroundCoroutine.async(Dispatchers.IO) {
            val (preciseFiles, vagueFiles) = searchExternalLyricsFiles(songFile, song)
            try {
                // precise
                Pair(
                    preciseFiles.mapNotNull { parseExternal(it, LyricsSource.ExternalPrecise()) },
                    vagueFiles.mapNotNull { parseExternal(it, LyricsSource.ExternalDecorated()) }
                )
            } catch (e: Exception) {
                ErrorNotification.postErrorNotification("Failed to read lyrics files\n${e.message}", App.instance)
                Pair(emptyList(), emptyList())
            }
        }

        val resultList: ArrayList<AbsLyrics> = ArrayList(4)
        resultList.apply {
            val embeddedLyrics = embedded.await()
            if (embeddedLyrics != null) {
                add(embeddedLyrics)
            }
            val (preciseLyrics, vagueLyrics) = external.await()
            addAll(preciseLyrics)
            addAll(vagueLyrics)
        }

        // end of fetching
        return LyricsList(resultList)
    }

    private fun parseEmbedded(
        songFile: File,
        lyricsSource: LyricsSource = LyricsSource.Embedded(),
    ): AbsLyrics? = try {
        AudioFileIO.read(songFile).tag?.getFirst(FieldKey.LYRICS).let { str ->
            if (str != null && str.trim().isNotBlank()) {
                parse(str, lyricsSource)
            } else {
                null
            }
        }
    } catch (e: CannotReadException) {
        val suffix = songFile.name.substringAfterLast('.', "")
        if (ErrorMessage.NO_READER_FOR_THIS_FORMAT.getMsg(suffix) != e.message) {
            ErrorNotification.postErrorNotification(e, "Failed to read song file\n", App.instance)
        }
        null
    } catch (e: Exception) {
        ErrorNotification.postErrorNotification(e,
                                                "Failed to read lyrics from song\n",
                                                App.instance)
        null
    }

    private fun parseExternal(
        file: File,
        lyricsSource: LyricsSource = LyricsSource.Unknown(),
    ): AbsLyrics? =
        file.readText().let { content ->
            if (content.isNotEmpty()) parse(content, lyricsSource) else null
        }

    private fun parse(raw: String, lyricsSource: LyricsSource = LyricsSource.Unknown()): AbsLyrics {
        val lines = raw.take(80).lines()
        val regex = Regex("""(\[.+])+.*""")

        for (line in lines) {
            if (regex.matches(line)) {
                return LrcLyrics.from(raw, lyricsSource)
            }
        }
        return TextLyrics.from(raw, lyricsSource)
    }

    fun searchExternalLyricsFiles(songFile: File, song: Song): Pair<List<File>, List<File>> {
        val dir = songFile.absoluteFile.parentFile ?: return emptyPair

        if (!dir.exists() || !dir.isDirectory) return emptyPair

        val filename = Regex.escape(FileUtil.stripExtension(songFile.name))
        val songName = Regex.escape(song.title)

        // precise pattern
        val preciseRegex = Regex("""$filename\.(lrc|txt)""", RegexOption.IGNORE_CASE)
        // vague pattern
        val vagueRegex1 = Regex(""".*[-;]?$filename[-;]?.*\.(lrc|txt)""", RegexOption.IGNORE_CASE)
        val vagueRegex2 = Regex(""".*[-;]?$songName[-;]?.*\.(lrc|txt)""", RegexOption.IGNORE_CASE)

        val preciseFiles: MutableList<File> = ArrayList(1)
        val vagueFiles: MutableList<File> = ArrayList(3)

        // start list file under the same dir
        val result = dir.listFiles { f: File ->
            when {
                preciseRegex.matches(f.name) -> {
                    preciseFiles.add(f)
                    if (DEBUG) Log.v(TAG, "add a precise file: ${f.path} for ${song.title}")
                    return@listFiles true
                }
                vagueRegex1.matches(f.name)  -> {
                    vagueFiles.add(f)
                    if (DEBUG) Log.v(TAG, "add a vague file: ${f.path} for ${song.title}")
                    return@listFiles true
                }
                vagueRegex2.matches(f.name)  -> {
                    vagueFiles.add(f)
                    if (DEBUG) Log.v(TAG, "add a vague file: ${f.path} for ${song.title}")
                    return@listFiles true
                }
            }
            false
        }

        return if (result.isNullOrEmpty()) {
            emptyPair
        } else {
            if (DEBUG) Log.v(TAG,
                             result.fold("All lyrics found:") { acc, str -> "$acc;${str.path}" })
            Pair(preciseFiles, vagueFiles)
        }
    }

    private const val TAG = "LyricsLoader"
    private val emptyPair: Pair<List<File>, List<File>> get() = Pair(emptyList(), emptyList())
}
