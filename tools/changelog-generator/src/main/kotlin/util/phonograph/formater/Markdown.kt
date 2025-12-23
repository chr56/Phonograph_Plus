/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.formater

abstract class MarkdownFormater {

    protected open fun border(text: String) = "**$text**"

    protected fun title(text: String, level: Int) = "${"#".repeat(level)} $text\n"

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

    /**
     * Escapes Markdown characters
     */
    protected fun escapeMarkdown(origin: String): String =
        Regex("""([_*\[\]()~`>\#\+\-=|\.!\{\}])""").replace(origin) { "\\${it.value}" }

    /**
     * Escapes MarkdownV2 characters
     */
    protected fun escapeMarkdownV2(origin: String): String =
        Regex("""([\[\]()\#\+\-=|\.!])""").replace(origin) { "\\${it.value}" }

    /**
     * Only for Github
     */
    protected fun githubAlertBox(text: String, type: String = "NOTE"): String =
        buildString {
            append("> [!$type]")
            append('\n')
            for (line in text.lines()) {
                append("> ")
                append(line)
                append('\n')
            }
        }
}
