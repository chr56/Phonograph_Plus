/*
 * Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.lyrics

import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LyricsSource
import player.phonograph.model.lyrics.TextLyrics
import kotlinx.parcelize.Parcelize
import java.util.regex.Pattern

@Parcelize
data class ActualTextLyrics(
    private val lines: List<String>,
    override val source: LyricsSource,
    override var title: String = AbsLyrics.DEFAULT_TITLE,
) : TextLyrics {

    override val raw: String get() = lines.joinToString(separator = "\r\n") { it.trim() }
    override val length: Int get() = lines.size
    override val lyricsLineArray: Array<String> get() = Array(lines.size) { lines[it] }
    override val lyricsTimeArray: IntArray get() = IntArray(lines.size) { -1 }

    companion object {
        private const val TAG = "TextLyrics"
        fun from(raw: String, source: LyricsSource = LyricsSource.Unknown): ActualTextLyrics {
            val result = raw.split(Pattern.compile("(\r?\n)|(\\\\[nNrR])"))
            return ActualTextLyrics(result.toMutableList(), source)
        }
    }
}
