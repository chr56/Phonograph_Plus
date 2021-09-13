package com.kabouzeid.gramophone.model.lyrics

import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.FileUtil
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.lang.Exception
import java.lang.NumberFormatException
import java.util.Locale
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class LyricsParsed private constructor() {
    var lyrics: ArrayList<LyricsLine>? = null
    private constructor(lyrics: ArrayList<LyricsLine>) : this() {
        this.lyrics = lyrics
    }


    companion object{

        fun parse(raw: String):LyricsParsed{
            val lines: List<String?> = raw.split(Pattern.compile("\r?\n"))
            val lyrics: MutableList<LyricsLine> = ArrayList()
            lines.forEach {
                lyrics.add(LyricsLine(it.orEmpty()))
            }
            return LyricsParsed(lyrics as ArrayList<LyricsLine>)
        }


        /**
         * create parsed lyrics via song
         * @throws Exception (no lyrics found)
         * @author Karim Abou Zeid (kabouzeid), chr_56
         */
        @JvmStatic
        fun parse(song: Song): LyricsParsed{
            val file = File(song.data)

            var rawLyrics: String? = null
            // Read from file's Tag
            try {
                rawLyrics = AudioFileIO.read(file).tagOrCreateDefault.getFirst(FieldKey.LYRICS)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Read from .lrc/.txt with same name
            if (rawLyrics == null || rawLyrics.trim().isEmpty()){
                val dir = file.absoluteFile.parentFile

                if (dir != null && dir.exists() && dir.isDirectory){
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

    }
}