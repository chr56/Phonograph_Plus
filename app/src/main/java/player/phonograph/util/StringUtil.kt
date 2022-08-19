/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util

import android.content.Context
import android.text.Html
import android.text.Spanned
import player.phonograph.R
import java.lang.StringBuilder

object StringUtil {
    fun buildDeletionMessage(context: Context, itemSize: Int, extraSuffix: CharSequence?, vararg data: ItemGroup): Spanned {
        return buildSomeMessage(
            header = context.resources.getQuantityString(R.plurals.msg_header_delete_items, itemSize),
            content = data,
            end = extraSuffix,
            warning = context.getString(R.string.warning_can_not_retract),
        )
    }

    class ItemGroup(
        val typeName: String,
        val items: List<String>,
    )

    fun buildSomeMessage(
        header: CharSequence,
        content: Array<out ItemGroup>,
        end: CharSequence?,
        warning: CharSequence?,
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
