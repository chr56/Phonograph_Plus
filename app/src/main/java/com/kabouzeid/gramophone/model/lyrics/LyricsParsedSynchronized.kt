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

class LyricsParsedSynchronized private constructor() {
    var lyrics: ArrayList<LyricsLineSynchronized>? = null
    var offset: Long =0
    private constructor(lyrics: ArrayList<LyricsLineSynchronized>, offset: Long) : this() {
        this.lyrics = lyrics
        this.offset = offset
    }


    companion object{
        private val LRC_LINE_PATTERN = Pattern.compile("((?:\\[.*?\\])+)(.*)")
        private val LRC_TIME_PATTERN = Pattern.compile("\\[(\\d+):(\\d{2}(?:\\.\\d+)?)\\]")
        private val LRC_ATTRIBUTE_PATTERN = Pattern.compile("\\[(\\D+):(.+)\\]")
        private const val LRC_SECONDS_TO_MS_MULTIPLIER = 1000f
        private const val LRC_MINUTES_TO_MS_MULTIPLIER = 60000

        /**
         * create parsed lyrics via raw string (from a file)
         * @author Karim Abou Zeid (kabouzeid), chr_56
         */
        @JvmStatic
        fun parse(rawLyrics: String): LyricsParsedSynchronized{
            // raw data:
            //
            val lines: List<String> = rawLyrics.split(Pattern.compile("\r?\n"))
            var offset: Long =0
            var title: String? =null
            // data to Return:
            //
            var lyrics: MutableList<LyricsLineSynchronized> = ArrayList()


            for (line in lines) {
                // blank line ?
                if (line.isBlank()) {
                    continue
                }
                // parse metadata of lyric
                val attrMatcher = LRC_ATTRIBUTE_PATTERN.matcher(line)
                if (attrMatcher.find()) {
                    try { //todo [lyric parse] fix null safe in attr parse
                        val attr = attrMatcher.group(1)!!.lowercase(Locale.ENGLISH).trim { it <= ' ' }
                        val value = attrMatcher.group(2)!!.lowercase(Locale.ENGLISH).trim { it <= ' ' }
                        when (attr) {
                            "offset" -> offset = value.toLong()
                            "ti" -> title = value
                            // todo [lyric parse] more attr
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                // parse lyric lines
                val matcher = LRC_LINE_PATTERN.matcher(line)
                if (matcher.find()) {
                    val time: String? = matcher.group(1)
                    val text: String? = matcher.group(2)

                    var ms: Long = 0

                    //Time
                    try {//todo: null safe?
                        val timeMatcher = LRC_TIME_PATTERN.matcher(time!!)
                        while (timeMatcher.find()) {
                            var m = 0
                            var s = 0f
                            try {//todo [lyric parse] fix null safe in line parse
                                m = timeMatcher.group(1)!!.toInt()
                                s = timeMatcher.group(2)!!.toFloat()
                            } catch (ex: NumberFormatException) {
                                ex.printStackTrace()
                            }
                            ms = ((s * LRC_SECONDS_TO_MS_MULTIPLIER).toInt() + m * LRC_MINUTES_TO_MS_MULTIPLIER).toLong()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    //Text
                    //

                    //write to var
                    //
                    lyrics.add(LyricsLineSynchronized(ms,text.orEmpty()))


                }
            } // loop_end

            // create lyrics
            return LyricsParsedSynchronized(lyrics as ArrayList<LyricsLineSynchronized>, offset)
        }


        /**
         * create parsed lyrics via song
         * @throws Exception (no lyrics found)
         * @author Karim Abou Zeid (kabouzeid), chr_56
         */
        @JvmStatic
        fun parse(song: Song): LyricsParsedSynchronized{
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
                    val format = ".*%s.*\\.(lrc)" //Todo
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
            return parse(rawLyrics!!)//TODO;
        }
        /**
         * create parsed lyrics via file
         */
        @JvmStatic
        fun parse(file: File): LyricsParsedSynchronized{
            return parse(FileUtil.read(file))
        }
    }
}