/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output

import util.phonograph.format.dateString
import util.phonograph.releasenote.Notes
import util.phonograph.releasenote.ReleaseNote
import java.io.Writer

abstract class Markdown : OutputFormat {

    protected fun border(text: String) = "**$text**"

    protected fun title(text: String, level: Int) = "${"#".repeat(level)} $text"

    protected fun makeUnorderedList(items: List<String>) = buildString {
        for (item in items) {
            appendLine("- $item")
        }
    }

    protected fun makeOrderedList(items: List<String>) = buildString {
        for ((index, item) in items.withIndex()) {
            appendLine("${index + 1}. $item")
        }
    }
}


class GitHubReleaseMarkdown(private val releaseNote: ReleaseNote) : Markdown() {

    @Suppress("SameParameterValue")
    private fun section(note: Notes.Note, title: String, level: Int): String = buildString {
        appendLine(title(title, level)).append('\n')
        appendLine(makeUnorderedList(note.highlights)).append('\n')
        appendLine(makeOrderedList(note.items)).append('\n')
    }


    override fun write(target: Writer) {

        val title = title(border("v${releaseNote.version} ${dateString(releaseNote.timestamp)}"), 2)
        val en = section(releaseNote.notes.en, "EN", 3)
        val zh = section(releaseNote.notes.zh, "ZH", 3)
        val extra = "**Commit log**: "

        target.append(title).append('\n').append('\n')
        target.append(en).append('\n').append('\n')
        target.append(zh).append('\n').append('\n')
        target.append(extra).append('\n').append('\n')

    }
}

class IMReleaseMarkdown(private val releaseNote: ReleaseNote) : Markdown() {

    private fun section(note: Notes.Note, title: String): String = buildString {
        appendLine(border(title)).append('\n')
        appendLine(makeUnorderedList(note.highlights)).append('\n')
        appendLine(makeOrderedList(note.items)).append('\n')
    }

    override fun write(target: Writer) {

        val title = border("v${releaseNote.version} ${dateString(releaseNote.timestamp)}")
        val en = section(releaseNote.notes.en, "EN")
        val zh = section(releaseNote.notes.zh, "ZH")
        val extra = "**Commit log**: "

        target.append(title).append('\n')
        target.append(en).append('\n')
        target.append(zh).append('\n')
        target.append(extra).append('\n')

    }
}