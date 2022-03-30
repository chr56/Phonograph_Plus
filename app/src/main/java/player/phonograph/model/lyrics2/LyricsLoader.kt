/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics2

import android.util.Log
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import player.phonograph.BuildConfig
import player.phonograph.util.FileUtil
import java.io.File
import java.util.regex.Pattern

data class LyricsPack(val embedded: AbsLyrics?, val external: AbsLyrics?, val externalWithSuffix: AbsLyrics?)

object LyricsLoader {

    fun loadLyrics(songFile: File, songTitle: String): LyricsPack {

        // embedded
        var embedded: AbsLyrics? = null
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

        // external
        var external: AbsLyrics? = null
        var externalWithSuffix: AbsLyrics? = null
        songFile.absoluteFile.parentFile?.let { dir ->
            if (dir.exists() && dir.isDirectory) {
                val filename = Pattern.quote(FileUtil.stripExtension(songFile.name))

                // precise pattern
                val preciseFormat = "%s\\.(lrc|txt)"
                val precisePattern = Pattern.compile(String.format(preciseFormat, filename), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)

                // vague pattern
                val vagueFormat = ".*[-;]?%s[-;]?.*\\.(lrc|txt)"
                val vaguePatterns = listOf<Pattern>(
                    Pattern.compile(String.format(vagueFormat, filename), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE),
                    Pattern.compile(String.format(vagueFormat, songTitle), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
                )
                val preciseFiles: MutableList<File> = ArrayList(2)
                val vagueFiles: MutableList<File> = ArrayList(6)

                // start list file under the same dir
                dir.listFiles { f: File ->
                    // precise match
                    if (precisePattern.matcher(f.name).matches()) {
                        preciseFiles.add(f)
                        Log.v(TAG, "add a precise file: ${f.path} for $songTitle")
                        return@listFiles true
                    }
                    // vague match
                    for (pattern in vaguePatterns) {
                        if (pattern.matcher(f.name).matches()) {
                            vagueFiles.add(f)
                            Log.v(TAG, "add a vague file: ${f.path} for $songTitle")
                            return@listFiles true
                        }
                    }
                    false
                }?.also { allMatchedFiles ->

                    Log.v(TAG, allMatchedFiles.map { it.path }.fold("All lyrics found:") { acc, str -> "$acc$str;" }.toString())

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
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read lyrics files\n${e.message}")
                    }
                }
            }
        }
        // end of fetching
        return LyricsPack(embedded, external, externalWithSuffix)
    }

//    private val regex: Regex by lazy { "(\\[.+\\])+.*".toRegex(RegexOption.MULTILINE) }
    private val pattern: Pattern by lazy { Pattern.compile("(\\[.+\\])+.*", Pattern.MULTILINE) }
    fun parse(raw: String): AbsLyrics {

        pattern.matcher(
            raw.take(80) /* sample string */
        ).find()
            .also { result ->
                return if (result) {
                    Log.v(TAG, "using LrcLyrics")
                    LrcLyrics.from(raw)
                } else {
                    Log.v(TAG, "using TextLyrics")
                    TextLyrics.from(raw)
                }
            }
    }
    private const val TAG = "LyricsLoader"
}
