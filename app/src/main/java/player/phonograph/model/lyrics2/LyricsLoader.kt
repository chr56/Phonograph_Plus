/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics2

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import player.phonograph.BuildConfig
import player.phonograph.util.FileUtil
import java.io.File

object LyricsLoader {

    private val backgroundCoroutine: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    suspend fun loadLyrics(songFile: File, songTitle: String): LyricsSet {

        // embedded
        var embedded: AbsLyrics? = null

        val jobEmbedded = backgroundCoroutine.launch(Dispatchers.IO) {
            try {
                AudioFileIO.read(songFile).tag?.getFirst(FieldKey.LYRICS).also { str ->
                    if (str != null && str.trim().isNotBlank()) {
                        embedded = parse(str)
                    }
                }
            } catch (e: Exception) {
                val buildType = BuildConfig.BUILD_TYPE
                if (buildType != "release" || buildType != "preview") { Log.e(TAG, "Failed to read lyrics from song\n${e.message}") }
            }
        }

        // external
        var external: AbsLyrics? = null
        var externalWithSuffix: AbsLyrics? = null

        val jobExternal = backgroundCoroutine.launch(Dispatchers.IO) {
            songFile.absoluteFile.parentFile?.let { dir ->
                if (dir.exists() && dir.isDirectory) {
                    val filename = Regex.escape(FileUtil.stripExtension(songFile.name))
                    val songName = Regex.escape(songTitle)

                    // precise pattern
                    val preciseRegex = Regex("""$filename\.(lrc|txt)""", RegexOption.IGNORE_CASE)
                    // vague pattern
                    val vagueRegex1 = Regex(""".*[-;]?$filename[-;]?.*\.(lrc|txt)""", RegexOption.IGNORE_CASE)
                    val vagueRegex2 = Regex(""".*[-;]?$songName[-;]?.*\.(lrc|txt)""", RegexOption.IGNORE_CASE)

                    val preciseFiles: MutableList<File> = ArrayList(2)
                    val vagueFiles: MutableList<File> = ArrayList(6)

                    // start list file under the same dir
                    val result = dir.listFiles { f: File ->
                        when {
                            preciseRegex.matches(f.name) -> {
                                preciseFiles.add(f)
                                Log.v(TAG, "add a precise file: ${f.path} for $songTitle")
                                return@listFiles true
                            }
                            vagueRegex1.matches(f.name) -> {
                                vagueFiles.add(f)
                                Log.v(TAG, "add a vague file: ${f.path} for $songTitle")
                                return@listFiles true
                            }
                            vagueRegex2.matches(f.name) -> {
                                vagueFiles.add(f)
                                Log.v(TAG, "add a vague file: ${f.path} for $songTitle")
                                return@listFiles true
                            }
                        }
                        false
                    }

                    if (result.isNullOrEmpty()) return@let

                    Log.v(TAG, result.map { it.path }.fold("All lyrics found:") { acc, str -> "$acc$str;" }.toString())

                    try {
                        // precise first
                        for (f in preciseFiles) {
                            FileUtil.read(f).also { str ->
                                if (str.isNotEmpty()) external = parse(str)
                            }
                        }
                        // then vague
                        for (f in vagueFiles) {
                            FileUtil.read(f).also { str ->
                                if (str.isNotEmpty()) externalWithSuffix = parse(str)
                            }
                        }
                    } catch (e: Exception) { Log.e(TAG, "Failed to read lyrics files\n${e.message}") }
                }
            }
        }

        // wait to join
        jobExternal.join()
        jobEmbedded.join()

        // todo
        val embeddedPack =
            if (embedded != null) Lyrics(embedded!!, LyricsSource.Embedded()) else null
        val externalPack: MutableList<Lyrics> = ArrayList()
        if (external != null)
            externalPack.add(
                Lyrics(external!!, LyricsSource.ExternalPrecise())
            )
        if (externalWithSuffix != null)
            externalPack.add(
                Lyrics(externalWithSuffix!!, LyricsSource.ExternalDecorated())
            )

        // end of fetching
        return LyricsSet(embeddedPack, if (externalPack.isEmpty()) null else externalPack)
    }

    fun parse(raw: String): AbsLyrics {
        val lines = raw.take(80).lines()
        val regex = Regex("""(\[.+])+.*""")

        for (line in lines) {
            if (regex.matches(line)) {
                Log.v(TAG, "using LrcLyrics")
                return LrcLyrics.from(raw)
            }
        }
        Log.v(TAG, "using TextLyrics")
        return TextLyrics.from(raw)
    }

    private const val TAG = "LyricsLoader"
}
