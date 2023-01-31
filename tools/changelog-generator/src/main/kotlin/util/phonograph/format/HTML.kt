/*
 * Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.format

var baseIndent = 4

fun html(block: HTMLContext.() -> Unit): List<String> {
    val context = HTMLContext(mutableListOf(), 0)
    context.apply(block)
    return context.export()
}

class HTMLContext(private val container: MutableList<String>, private val currentIndentLevel: Int) {
    fun line(string: String, extraIndentLevel: Int = 0) {
        container.add("${" ".repeat(baseIndent * (currentIndentLevel + extraIndentLevel))}$string")
    }

    fun lines(strings: List<String>, extraIndentLevel: Int = 0) {
        for (str in strings) line(str, extraIndentLevel)
    }

    fun wrapWith(tag: String, extraIndentLevel: Int = 0, block: HTMLContext.() -> Unit) {
        val context = HTMLContext(mutableListOf(), currentIndentLevel)
        context.apply(block)
        line("<$tag>")
        lines(context.export(), 1 + extraIndentLevel)
        line("</$tag>")
    }

    fun export() = container
}

fun HTMLContext.div(extraIndentLevel: Int = 0, block: HTMLContext.() -> Unit) {
    wrapWith("div", extraIndentLevel, block)
}

fun HTMLContext.htmlNoteItem(items: List<String>, extraIndentLevel: Int = 0) {
    wrapWith("ol", extraIndentLevel) {
        for (item in items) {
            line("<li>$item</li>", 0)
        }
    }
}