/*
 *  Copyright (c) 2022~2025 chr_56
 */

package util.phonograph.formater

fun html(block: HTMLBuilder.() -> Unit): List<String> {
    val context = HTMLBuilder(mutableListOf(), 0)
    context.apply(block)
    return context.export()
}

class HTMLBuilder(
    private val container: MutableList<String>,
    private val currentIndentLevel: Int = 0,
    private val baseIndent: Int = 4,
) {
    fun line(string: String, extraIndentLevel: Int = 0) {
        container.add("${" ".repeat(baseIndent * (currentIndentLevel + extraIndentLevel))}$string")
    }

    fun lines(strings: List<String>, extraIndentLevel: Int = 0) {
        for (str in strings) line(str, extraIndentLevel)
    }

    fun wrapWith(tag: String, extraIndentLevel: Int = 0, block: HTMLBuilder.() -> Unit) {
        val context = HTMLBuilder(mutableListOf(), currentIndentLevel)
        context.apply(block)
        line("<$tag>")
        lines(context.export(), 1 + extraIndentLevel)
        line("</$tag>")
    }

    fun div(extraIndentLevel: Int = 0, block: HTMLBuilder.() -> Unit) {
        wrapWith("div", extraIndentLevel, block)
    }

    fun ordered(items: List<String>, extraIndentLevel: Int = 0) {
        wrapWith("ol", extraIndentLevel) {
            for (item in items) {
                line("<li>$item</li>", 0)
            }
        }
    }

    fun export() = container
}