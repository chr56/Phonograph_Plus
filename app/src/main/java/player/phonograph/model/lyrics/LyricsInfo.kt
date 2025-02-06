/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.lyrics

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class LyricsInfo(
    private val lyricsList: List<AbsLyrics>,
    private val activatedLyricsNumber: Int,
) : Parcelable, List<AbsLyrics> by lyricsList {


    fun firstLrcLyrics(): LrcLyrics? = findFirst<LrcLyrics>()
    fun firstTextLyrics(): TextLyrics? = findFirst<TextLyrics>()
    fun allLyricsFrom(source: LyricsSource): List<AbsLyrics> = lyricsList.filter { it.source == source }
    fun firstLyricsFrom(source: LyricsSource): AbsLyrics? = allLyricsFrom(source).firstOrNull()
    fun availableSources(): Set<LyricsSource> = lyricsList.map { it.source }.toSet()
    val activatedLyrics: AbsLyrics? get() = lyricsList.getOrNull(activatedLyricsNumber)

    fun isActive(index: Int) = index == activatedLyricsNumber

    private inline fun <reified L> findFirst(): L? {
        for (lyric in lyricsList) {
            if (lyric is L) return lyric
        }
        return null
    }

    fun createWithAppended(absLyrics: AbsLyrics): LyricsInfo = withAppended(this, absLyrics)

    fun createWithActivated(index: Int): LyricsInfo = withActivated(this, index)
    fun createWithActivated(lyrics: AbsLyrics): LyricsInfo? = withActivated(this, lyrics)

    companion object {

        fun withAppended(old: LyricsInfo, newLyrics: AbsLyrics): LyricsInfo =
            LyricsInfo(
                old.lyricsList + listOf(newLyrics),
                old.activatedLyricsNumber
            )

        fun withActivated(old: LyricsInfo, index: Int): LyricsInfo =
            LyricsInfo(
                old.lyricsList,
                index
            )

        fun withActivated(old: LyricsInfo, lyrics: AbsLyrics): LyricsInfo? {
            var index = -1
            for ((i, l) in old.lyricsList.withIndex()) {
                if (l === lyrics) index = i
            }
            return if (index > -1) withActivated(old, index) else null
        }
    }
}