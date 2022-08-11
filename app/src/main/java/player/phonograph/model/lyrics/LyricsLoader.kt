/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import player.phonograph.BuildConfig
import player.phonograph.model.Song
import player.phonograph.util.FileUtil
import java.io.File

object LyricsLoader {

    private val backgroundCoroutine: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    suspend fun loadLyrics(songFile: File, song: Song): LyricsList {

        // embedded
        var embedded: AbsLyrics? = null

        val jobEmbedded = backgroundCoroutine.launch(Dispatchers.IO) {
            try {
                AudioFileIO.read(songFile).tag?.getFirst(FieldKey.LYRICS).also { str ->
                    if (str != null && str.trim().isNotBlank()) {
                        embedded = parse(str, LyricsSource.Embedded())
                    }
                }
            } catch (e: Exception) {
                val buildType = BuildConfig.BUILD_TYPE
                if (buildType != "release" || buildType != "preview") { Log.e(TAG, "Failed to read lyrics from song\n${e.message}") }
            }
        }

        // external
        val preciseLyrics: MutableList<AbsLyrics> = ArrayList(1)
        val vagueLyrics: MutableList<AbsLyrics> = ArrayList(3)

        val jobExternal = backgroundCoroutine.launch(Dispatchers.IO) {
            songFile.absoluteFile.parentFile?.let { dir ->
                if (dir.exists() && dir.isDirectory) {
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
                                Log.v(TAG, "add a precise file: ${f.path} for ${song.title}")
                                return@listFiles true
                            }
                            vagueRegex1.matches(f.name) -> {
                                vagueFiles.add(f)
                                Log.v(TAG, "add a vague file: ${f.path} for ${song.title}")
                                return@listFiles true
                            }
                            vagueRegex2.matches(f.name) -> {
                                vagueFiles.add(f)
                                Log.v(TAG, "add a vague file: ${f.path} for ${song.title}")
                                return@listFiles true
                            }
                        }
                        false
                    }

                    if (result.isNullOrEmpty()) return@let

                    Log.v(TAG, result.map { it.path }.fold("All lyrics found:") { acc, str -> "$acc$str;" }.toString())

                    try {
                        // precise
                        for (f in preciseFiles) {
                            FileUtil.read(f).also { raw ->
                                if (raw.isNotEmpty()) preciseLyrics.add(parse(raw, LyricsSource.ExternalPrecise()))
                            }
                        }
                        // vague
                        for (f in vagueFiles) {
                            FileUtil.read(f).also { raw ->
                                if (raw.isNotEmpty()) vagueLyrics.add(parse(raw, LyricsSource.ExternalDecorated()))
                            }
                        }
                    } catch (e: Exception) { Log.e(TAG, "Failed to read lyrics files\n${e.message}") }
                }
            }
        }

        // wait to join
        jobExternal.join()
        jobEmbedded.join()

        val resultList: ArrayList<AbsLyrics> = ArrayList(4)
        resultList.apply {
            if (embedded != null) add(embedded!!)
            addAll(preciseLyrics)
            addAll(vagueLyrics)
        }

        // end of fetching
        return LyricsList(resultList)
    }

    fun parse(raw: String, lyricsSource: LyricsSource = LyricsSource.Unknown()): AbsLyrics {
        val lines = raw.take(80).lines()
        val regex = Regex("""(\[.+])+.*""")

        for (line in lines) {
            if (regex.matches(line)) {
                Log.v(TAG, "using LrcLyrics")
                return LrcLyrics.from(raw, lyricsSource)
            }
        }
        Log.v(TAG, "using TextLyrics")
        return TextLyrics.from(raw, lyricsSource)
    }

    private const val TAG = "LyricsLoader"
}
