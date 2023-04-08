/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

import android.content.Context
import android.text.Html
import android.text.Spanned
import player.phonograph.R

object StringUtil {

    fun getSectionName(reference: String?): String {
        if (reference.isNullOrBlank()) return ""
        var str = reference.trim { it <= ' ' }.lowercase()
        str = when {
            str.startsWith("the ") -> str.substring(4)
            str.startsWith("a ") -> str.substring(2)
            else -> str
        }
        return if (str.isEmpty()) "" else str[0].uppercase()
    }

    fun buildRemovalMessage(
        context: Context,
        itemSize: Int,
        where: CharSequence,
        suffix: CharSequence?,
        vararg data: ItemGroup
    ): Spanned {
        val res = context.resources
        return buildSomeMessage(
            header =
            res.getQuantityString(R.plurals.msg_header_remove_items, itemSize, where),
            content = data,
            end = suffix,
            warning = res.getString(R.string.warning_can_not_retract)
        )
    }

    fun buildDeletionMessage(
        context: Context,
        itemSize: Int,
        extraSuffix: CharSequence?,
        vararg data: ItemGroup
    ): Spanned {
        val res = context.resources
        return buildSomeMessage(
            header =
            res.getQuantityString(R.plurals.msg_header_delete_items, itemSize),
            content = data,
            end = extraSuffix,
            warning = res.getString(R.string.warning_can_not_retract)
        )
    }

    class ItemGroup(
        val typeName: String,
        val items: List<String>
    )

    fun buildSomeMessage(
        header: CharSequence,
        content: Array<out ItemGroup>,
        end: CharSequence?,
        warning: CharSequence?
    ): Spanned {
        val s = StringBuilder()

        for (group in content) {
            if (group.items.isNotEmpty()) {
                s.append("${group.typeName}<br/>")
                for (i in group.items) {
                    s.append("* <b>$i</b><br/>")
                }
            }
        }

        val message = """
           <b>$header</b><br />
           $s
           <br/>
           <b>$warning</b><br/>
           <br/>$end
        """.trimIndent()
        return Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY)
    }
}
