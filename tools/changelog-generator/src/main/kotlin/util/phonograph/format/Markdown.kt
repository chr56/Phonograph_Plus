/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.format

import util.phonograph.ReleaseNoteModel

internal fun ReleaseNoteModel.markdownHeader() = "## **v${version} ${dateString(time)}**"
internal fun markdownNoteSubtitle(text: String) = "### $text"
internal fun markdownNoteItem(items: List<String>) = buildString {
    var index = 1
    for (item in items) {
        appendLine("$index. $item")
        index++
    }
}