/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.lyrics

class LyricsCursor(val l: LrcLyrics) {

    private var index: Int = 0
    fun setIndex(i: Int) {
        index = i
    }

    fun locate(index: Int): String {
        return l.rawLyrics.valueAt(index) as String
    }

    fun next(): String {
        index++
        return l.rawLyrics.valueAt(index) as String
    }

    fun previous(): String {
        index--
        return l.rawLyrics.valueAt(index) as String
    }

    fun first(): String {
        return l.rawLyrics[0] as String
    }

    fun moveToFirst() {
        index = 0
    }

    fun last(): String {
        return l.rawLyrics[l.rawLyrics.size()] as String
    }

    fun moveToLast() {
        index = l.rawLyrics.size()
    }
}