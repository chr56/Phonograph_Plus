package com.kabouzeid.gramophone.model.lyrics

import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.FileUtil
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.regex.Pattern

class LyricsParsed private constructor() : AbsLyrics() {
    override var TYPE: Short = TXT

    private var lyrics: ArrayList<CharSequence>? = null
    private var title: CharSequence? = null

    private constructor(lyrics: ArrayList<CharSequence>) : this() {
        if (lyrics.isNullOrEmpty()) throw Exception("NO_LYRIC")
        this.lyrics = lyrics
    }

    override fun getText(): String {
        val stringBuilder = StringBuilder()
        lyrics?.forEach { line ->
            stringBuilder.append(line).append("\r\n")
        }
        return stringBuilder.toString().trim { it <= ' ' }.replace("(\r?\n){3,}".toRegex(), "\r\n\r\n")
    }

    override fun getTitle(): CharSequence {
        return title ?: super.getTitle()
    }


    companion object {

        /**
         *  @param raw : String that contain the entire file
         */
        @JvmStatic
        fun parse(raw: String): LyricsParsed {
            val lines: List<CharSequence> = raw.split(Pattern.compile("(\r?\n)|(\\\\[nNrR])"))
            val lyrics: MutableList<CharSequence> = MutableList(lines.size) { lines[it] }
            return LyricsParsed(lyrics as ArrayList<CharSequence>)
        }


        /**
         * create parsed lyrics via song
         * @throws Exception (no lyrics found)
         * @author Karim Abou Zeid (kabouzeid), chr_56
         */
        @JvmStatic
        fun parse(song: Song): LyricsParsed {
            val file = File(song.data)

            var rawLyrics: String? = null
            // Read from file's Tag
            try {
                rawLyrics = AudioFileIO.read(file).tagOrCreateDefault.getFirst(FieldKey.LYRICS)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Read from .lrc/.txt with same name
            if (rawLyrics == null || rawLyrics.trim().isEmpty()) {
                val dir = file.absoluteFile.parentFile

                if (dir != null && dir.exists() && dir.isDirectory) {
                    val format = ".*%s.*\\.(lrc|txt)" //Todo
                    val filename = Pattern.quote(FileUtil.stripExtension(file.name))
                    val patternForFile = Pattern.compile(String.format(format, filename), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)

                    val filesMatched = dir.listFiles { f: File ->
                        patternForFile.matcher(f.name).matches()
                    }

                    if (filesMatched != null && filesMatched.isNotEmpty()) {
                        for (f in filesMatched) {
                            try {
                                val rawLyricsFromFile = FileUtil.read(f)
                                if (rawLyricsFromFile != null && rawLyricsFromFile.trim { it <= ' ' }.isNotEmpty()) {
                                    rawLyrics = rawLyricsFromFile
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            //check success
            if (rawLyrics.isNullOrEmpty()) throw Exception("NO_LYRICS")//todo
            //create lyric
            return parse(rawLyrics)//TODO;
        }

        /**
         * create parsed lyrics via file
         */
        @JvmStatic
        fun parse(file: File): LyricsParsedSynchronized {
            return LyricsParsedSynchronized.parse(FileUtil.read(file))
        }

    }
}