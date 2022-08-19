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
        val content = StringBuilder()

        for (group in data) {
            if (group.items.isNotEmpty()) {
                content.append("${group.typeName}<br/>")
                for (i in group.items) {
                    content.append("* <b>$i</b><br/>")
                }
            }
        }

        val message = """
           <b>${context.resources.getQuantityString(R.plurals.msg_header_delete_items, itemSize)}</b><br />
           $content
           <br/>
           <b> ${context.getString(R.string.warning_can_not_retract)}</b><br/>
           <br/>$extraSuffix
        """.trimIndent()
        return Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY)
    }

    class ItemGroup(
        val typeName: String,
        val items: List<String>,
    )
}
