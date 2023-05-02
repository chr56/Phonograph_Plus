/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model.lyrics

import player.phonograph.model.Song
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class LyricsInfo(
    val linkedSong: Song,
    private val lyricsList: ArrayList<AbsLyrics>,
    private val activatedLyricsNumber: Int,
) : Parcelable, List<AbsLyrics> by lyricsList {


    fun firstLrcLyrics(): LrcLyrics? = findFirst<LrcLyrics>()
    fun firstTextLyrics(): TextLyrics? = findFirst<TextLyrics>()
    fun allLyricsFrom(source: LyricsSource): List<AbsLyrics> = lyricsList.filter { it.source == source }
    fun firstLyricsFrom(source: LyricsSource): AbsLyrics? = allLyricsFrom(source).firstOrNull()
    fun availableSources(): Set<LyricsSource> = lyricsList.map { it.source }.toSet()
    val activatedLyrics: AbsLyrics? get() = lyricsList.getOrNull(activatedLyricsNumber)

    private inline fun <reified L> findFirst(): L? {
        for (lyric in lyricsList) {
            if (lyric is L) return lyric
        }
        return null
    }

    fun createAmended(absLyrics: AbsLyrics): LyricsInfo = insect(this, absLyrics)

    fun replaceActivated(index: Int): LyricsInfo = setActive(this,index)
    fun replaceActivated(lyrics: AbsLyrics): LyricsInfo? = setActive(this,lyrics)

    companion object {
        val EMPTY = LyricsInfo(Song.EMPTY_SONG, ArrayList(), -1)

        private fun insect(old: LyricsInfo, newLyrics: AbsLyrics): LyricsInfo =
            LyricsInfo(
                old.linkedSong,
                ArrayList(old.lyricsList).also { it.add(newLyrics) },
                old.activatedLyricsNumber
            )

        private fun setActive(old: LyricsInfo, index: Int): LyricsInfo =
            LyricsInfo(
                old.linkedSong,
                old.lyricsList,
                index
            )

        private fun setActive(old: LyricsInfo, lyrics: AbsLyrics): LyricsInfo? {
            var index = -1
            for ((i, l) in old.lyricsList.withIndex()) {
                if (l === lyrics) {
                    index = i
                }
            }
            return if (index > -1) setActive(old, index) else null
        }
    }
}