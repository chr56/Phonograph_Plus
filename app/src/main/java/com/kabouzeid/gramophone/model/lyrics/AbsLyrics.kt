package com.kabouzeid.gramophone.model.lyrics

abstract class AbsLyrics {
    protected open var TYPE: Short = TXT
    abstract fun getText(): String
//    protected open lateinit var lyrics: ArrayList<out AbsLyricsLine>;
    open fun getTitle(): CharSequence{
        return "Lyrics"
    }


    companion object{
        const val LRC: Short = 2
        const val TXT: Short = 1
    }
}