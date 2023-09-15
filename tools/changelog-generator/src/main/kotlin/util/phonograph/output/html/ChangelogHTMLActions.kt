/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.output.html

import util.phonograph.dateString
import util.phonograph.releasenote.Language
import util.phonograph.releasenote.Notes
import util.phonograph.releasenote.ReleaseNote

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