/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.changelog

import util.phonograph.format.dateString
import util.phonograph.format.div
import util.phonograph.format.html
import util.phonograph.format.htmlNoteItem
import util.phonograph.format.markdownNoteItem
import util.phonograph.format.markdownNoteSubtitle

internal fun ReleaseNoteModel.markdownHeader() = "## **v${version} ${dateString(time)}**"
fun generateGitHubReleaseMarkDown(model: ReleaseNoteModel): String {

    val header = model.markdownHeader()
    val extra = "**Commit log**: "

    val zh = buildString {
        appendLine(markdownNoteSubtitle("ZH"))
        appendLine(markdownNoteItem(model.note.zh))
    }

    val en = buildString {
        appendLine(markdownNoteSubtitle("EN"))
        appendLine(markdownNoteItem(model.note.en))
    }

    return buildString {
        append(header).append('\n').append('\n')
        append(extra).append('\n').append('\n')
        append(zh).append('\n')
        append(en).append('\n')
    }
}

fun generateHTML(model: ReleaseNoteModel): Map<String, String> {
    val en = generateHTMLImpl(model.version, model.time, model.note.en).collect()
    val zh = generateHTMLImpl(model.version, model.time, model.note.zh).collect()
    return mapOf(
        "en" to en,
        "zh" to zh,
    )
}

private fun generateHTMLImpl(
    version: String,
    date: Long,
    items: List<String>
) = html {
    line(htmlHeader(version, date))
    div {
        htmlNoteItem(items)
    }
}

private fun htmlHeader(version: String, date: Long) =
    "<h4><b>$version</b> ${dateString(date)}</h4>"

private fun List<String>.collect(): String = reduce { acc, s -> "$acc\n$s" }