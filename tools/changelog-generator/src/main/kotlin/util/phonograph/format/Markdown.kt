/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.format

internal fun markdownNoteSubtitle(text: String) = "### $text"
internal fun markdownNoteItem(items: List<String>) = buildString {
    var index = 1
    for (item in items) {
        appendLine("$index. $item")
        index++
    }
}