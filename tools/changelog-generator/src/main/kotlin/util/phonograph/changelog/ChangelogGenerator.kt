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
    val extra = "**Commit log**:"

    val zh = buildString {
        appendLine(markdownNoteSubtitle(Language.Chinese.code.uppercase()))
        appendLine(markdownNoteItem(model.note.language(Language.Chinese)))
    }

    val en = buildString {
        appendLine(markdownNoteSubtitle(Language.English.code.uppercase()))
        appendLine(markdownNoteItem(model.note.language(Language.English)))
    }

    return buildString {
        append(header).append('\n').append('\n')
        append(extra).append('\n').append('\n')
        append(en).append('\n')
        append(zh).append('\n')
    }
}

fun generateTGReleaseMarkDown(model: ReleaseNoteModel): String {

    val header = "**v${model.version} ${dateString(model.time)}**"

    val zh = buildString {
        appendLine("**${Language.Chinese.code.uppercase()}**")
        appendLine(markdownNoteItem(model.note.language(Language.Chinese)))
    }

    val en = buildString {
        appendLine("**${Language.English.code.uppercase()}**")
        appendLine(markdownNoteItem(model.note.language(Language.English)))
    }

    return buildString {
        append(header).append('\n')
        append(zh).append('\n')
        append(en).append('\n')
    }
}

fun generateHTML(model: ReleaseNoteModel): Map<Language, String> {
    val en = generateHTMLImpl(model.version, model.time, model.note.language(Language.English)).collect()
    val zh = generateHTMLImpl(model.version, model.time, model.note.language(Language.Chinese)).collect()
    return mapOf(
        Language.English to en,
        Language.Chinese to zh,
    )
}

fun generateHTMLNoteMinify(note: ReleaseNoteModel.Note, lang: Language): String = html {
    htmlNoteItem(note.language(lang))
}.map { it.trimStart() }.reduce { acc, s -> "$acc$s" }.replace("\n", "\\n")

private fun generateHTMLImpl(
    version: String,
    date: Long,
    items: List<String>,
) = html {
    line(htmlHeader(version, date))
    div {
        htmlNoteItem(items)
    }
}

private fun htmlHeader(version: String, date: Long) =
    "<h4><b>$version</b> ${dateString(date)}</h4>"

private fun List<String>.collect(): String = reduce { acc, s -> "$acc\n$s" }