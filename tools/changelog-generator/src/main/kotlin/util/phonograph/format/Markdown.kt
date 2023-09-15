/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.format

internal fun markdownNoteSubtitle(text: String) = "### $text"

internal fun markdownNoteHighlight(highlights: List<String>) = buildString {
    for (item in highlights) {
        appendLine("- $item")
    }
}
internal fun markdownNoteItem(items: List<String>) = buildString {
    for ((index, item) in items.withIndex()) {
        appendLine("${index + 1}. $item")
    }
}