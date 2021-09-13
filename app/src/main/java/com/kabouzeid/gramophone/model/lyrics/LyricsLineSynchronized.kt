package com.kabouzeid.gramophone.model.lyrics

class LyricsLineSynchronized(timeStamp: Long, line: CharSequence) {
    private val timeStamp: Long = timeStamp
    fun getTimeStamp(): Long { return timeStamp }

    private val line: CharSequence = line
    fun getLine(): CharSequence { return line }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null ) return false;

        val  otherLine = other as LyricsLineSynchronized
        if (otherLine.line != this.line) return false
        if (otherLine.timeStamp != this.timeStamp) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return timeStamp.hashCode() * 31 + line.hashCode()
    }

    override fun toString(): String {
        return super.toString() + "#[" + timeStamp + "]" + line
    }
}