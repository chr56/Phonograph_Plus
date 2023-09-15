/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

import util.phonograph.format.dateString
import util.phonograph.format.div
import util.phonograph.format.html
import util.phonograph.format.htmlNoteItem
import util.phonograph.format.markdownNoteHighlight
import util.phonograph.format.markdownNoteSubtitle
import util.phonograph.releasenote.Language
import util.phonograph.releasenote.Notes
import util.phonograph.releasenote.ReleaseNote

fun generateGitHubReleaseMarkDown(model: ReleaseNote): String {

    val header = "## **v${model.version} ${dateString(model.timestamp)}**"

    val zh = buildString {
        val note = model.notes.zh
        appendLine(markdownNoteSubtitle("ZH")).append('\n')
        appendLine(markdownNoteHighlight(note.highlights)).append('\n')
        appendLine(markdownNoteHighlight(note.items)).append('\n')
    }

    val en = buildString {
        val note = model.notes.en
        appendLine(markdownNoteSubtitle("EN")).append('\n')
        appendLine(markdownNoteHighlight(note.highlights)).append('\n')
        appendLine(markdownNoteHighlight(note.items)).append('\n')
    }

    val extra = "**Commit log**: "

    return buildString {
        append(header).append('\n').append('\n')
        append(en).append('\n').append('\n')
        append(zh).append('\n').append('\n')
        append(extra).append('\n').append('\n')
    }
}

fun generateTGReleaseMarkDown(model: ReleaseNote): String {

    val header = "**v${model.version} ${dateString(model.timestamp)}**"


    val zh = buildString {
        val note = model.notes.zh
        appendLine("**ZH**").append('\n')
        appendLine(markdownNoteHighlight(note.highlights)).append('\n')
        appendLine(markdownNoteHighlight(note.items)).append('\n')
    }

    val en = buildString {
        val note = model.notes.en
        appendLine("**EN**").append('\n')
        appendLine(markdownNoteHighlight(note.highlights)).append('\n')
        appendLine(markdownNoteHighlight(note.items)).append('\n')
    }

    return buildString {
        append(header).append('\n').append('\n')
        append(zh).append('\n').append('\n')
        append(en).append('\n').append('\n')
    }
}

fun generateHTML(model: ReleaseNote): Map<Language, String> {


    val en = generateHTMLImpl(model.version, model.timestamp, model.language(Language.EN)).collect()
    val zh = generateHTMLImpl(model.version, model.timestamp, model.language(Language.ZH)).collect()
    return mapOf(
        Language.EN to en,
        Language.ZH to zh,
    )
}

fun generateHTMLNoteMinify(releaseNote: ReleaseNote, lang: Language): String =
    html {
        htmlNoteItem(releaseNote.language(lang).items)
    }.map { it.trimStart() }.reduce { acc, s -> "$acc$s" }.replace("\n", "\\n")

private fun generateHTMLImpl(
    version: String,
    timestamp: Long,
    note: Notes.Note,
) = html {
    line(htmlHeader(version, timestamp))
    div {
        htmlNoteItem(note.items)
    }
}

private fun htmlHeader(version: String, timestamp: Long) =
    "<h4><b>$version</b> ${dateString(timestamp)}</h4>"

private fun List<String>.collect(): String = reduce { acc, s -> "$acc\n$s" }