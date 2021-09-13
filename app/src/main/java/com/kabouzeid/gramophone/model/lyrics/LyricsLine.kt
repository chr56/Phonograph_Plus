package com.kabouzeid.gramophone.model.lyrics

class LyricsLine(line: CharSequence) {
    private val line: CharSequence = line
    fun getLine(): CharSequence { return line }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null ) return false;

        val  otherLine = other as LyricsLine
        if (otherLine.line != this.line) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return line.hashCode()
    }

    override fun toString(): String {
        return super.toString() + "#" + line
    }

}