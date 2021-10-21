package com.kabouzeid.phonograph.model.lyrics

abstract class AbsLyrics {
    protected open var TYPE: Short = TXT
    abstract fun getText(): String
    open fun getTitle(): CharSequence {
        return DEFAULT_TITLE
    }
    abstract fun getLyricsLineArray(): Array<CharSequence>
    abstract fun getLyricsTimeArray(): IntArray

    companion object {
        const val LRC: Short = 2
        const val TXT: Short = 1
        const val DEFAULT_TITLE = "Lyrics"
    }
}
