/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.lyrics

import player.phonograph.mechanism.metadata.JAudioTaggerExtractor
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.model.lyrics.LyricsInfo
import player.phonograph.model.lyrics.LyricsSource
import player.phonograph.util.debug
import player.phonograph.util.file.stripExtension
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.File

object LyricsLoader {

    /**
     * Parse raw lyrics content into AbsLyrics
     */
    fun parse(raw: String, lyricsSource: LyricsSource = LyricsSource.Unknown): AbsLyrics {
        val rawLength = raw.length
        val sampleLineCount = max(rawLength / 100, 10)
        val sampleLength = max(min(rawLength / 8, 480), 60)

        val lines = raw.take(sampleLength).splitToSequence("\r\n", "\n", "\r", limit = sampleLineCount)
        @Suppress("RegExpRedundantEscape") val regex = Regex("""(\[.+\])+\s*.*""")

        var score = 0
        for (line in lines) score += if (regex.matches(line)) 1 else -1

        return if (score > 1) ActualLrcLyrics.from(raw, lyricsSource) else ActualTextLyrics.from(raw, lyricsSource)
    }


    /**
     * Parse lyrics from [uri]
     */
    fun parse(contentResolver: ContentResolver, uri: Uri): AbsLyrics? =
        contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.reader().use {
                parse(it.readText(), LyricsSource.ManuallyLoaded)
            }
        }

    /**
     * Search lyrics for [songFile]
     */
    suspend fun search(songFile: File, songTitle: String): LyricsInfo {
        // embedded
        val embedded = withContext(SupervisorJob()) {
            val lyrics = JAudioTaggerExtractor.readLyrics(songFile)
            if (lyrics != null) parse(lyrics, LyricsSource.Embedded) else null
        }

        // external
        val externalPrecise = withContext(SupervisorJob()) {
            trySearch {
                searchExternalPreciseLyricsFiles(songFile).mapNotNull { file ->
                    val content = file.readText()
                    if (content.isNotEmpty()) parse(content, LyricsSource.ExternalPrecise) else null
                }
            }
        }
        val externalVague = withContext(SupervisorJob()) {
            trySearch {
                searchExternalVagueLyricsFiles(songFile, songTitle).mapNotNull { file ->
                    val content = file.readText()
                    if (content.isNotEmpty()) parse(content, LyricsSource.ExternalDecorated) else null
                }
            }
        }

        // collect
        val all = listOfNotNull(embedded) + externalPrecise + externalVague
        val activated: Int = all.indexOfFirst { it is LrcLyrics }

        // end of fetching
        return LyricsInfo(all, activated)
    }

    private fun <T> trySearch(block: () -> List<T>): List<T> =
        try {
            block()
        } catch (e: Exception) {
            debug {
                Log.e(TAG, "Failed to fetch lyrics", e)
            }
            emptyList<T>()
        }

    /**
     * search lyrics files associated with [songFile] precisely
     */
    fun searchExternalPreciseLyricsFiles(songFile: File): List<File> {
        val filename = stripExtension(songFile.absolutePath)
        val lrc = File("$filename.lrc").takeIf { it.exists() }
        val txt = File("$filename.txt").takeIf { it.exists() }
        return listOfNotNull(lrc, txt)
    }

    /**
     * search lyrics files associated with [songFile] vaguely
     */
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

    private const val TAG = "LyricsLoader"

}
