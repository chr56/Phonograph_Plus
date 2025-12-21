/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.html

import util.phonograph.formater.html
import util.phonograph.model.Language
import util.phonograph.model.ReleaseMetadata
import util.phonograph.utils.dateString

fun generateHTML(note: ReleaseMetadata): Map<Language, String> {
    val en = generateHTMLImpl(note.version, note.timestamp, note.language(Language.EN)).joinToString("\n")
    val zh = generateHTMLImpl(note.version, note.timestamp, note.language(Language.ZH)).joinToString("\n")
    return mapOf(
        Language.EN to en,
        Language.ZH to zh,
    )
}

fun generateHTMLNoteMinify(releaseMetadata: ReleaseMetadata, lang: Language): String =
    html {
        ordered(releaseMetadata.language(lang).items)
    }.map { it.trimStart() }.reduce { acc, s -> "$acc$s" }.replace("\n", "\\n")

private fun generateHTMLImpl(
    version: String,
    timestamp: Long,
    note: ReleaseMetadata.Notes.Note,
) = html {
    line(htmlHeader(version, timestamp))
    if (note.notice != null) {
        div {
            for (line in note.notice.lines()) {
                line(line)
            }
        }
    }
    div {
        ordered(note.items)
    }
}

private fun htmlHeader(version: String, timestamp: Long) =
    "<h4><b>$version</b> ${dateString(timestamp)}</h4>"