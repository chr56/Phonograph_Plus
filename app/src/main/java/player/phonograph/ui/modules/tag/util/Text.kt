/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.util

import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.model.metadata.Metadata
import player.phonograph.model.metadata.NOTATION_BINARY
import player.phonograph.model.metadata.NOTATION_BIT_RATE
import player.phonograph.model.metadata.NOTATION_COMPOSITE
import player.phonograph.model.metadata.NOTATION_DATA_SIZE
import player.phonograph.model.metadata.NOTATION_DURATION
import player.phonograph.model.metadata.NOTATION_EMPTY
import player.phonograph.model.metadata.NOTATION_NUMBER
import player.phonograph.model.metadata.NOTATION_RAW_TEXT
import player.phonograph.model.metadata.NOTATION_SAMPLING
import player.phonograph.model.metadata.NOTATION_TEXT
import player.phonograph.model.metadata.NOTATION_TIMESTAMP
import player.phonograph.util.text.dateTimeTextPrecise
import player.phonograph.util.text.detailedDuration
import player.phonograph.util.text.getFileSizeString
import android.content.Context

fun display(context: Context, field: Metadata.Field): String {
    val content = field.data
    val text = try {
        when (field.notation) {
            NOTATION_TEXT      -> limitedText(content as String)
            NOTATION_NUMBER    -> (content as Long).toString()
            NOTATION_TIMESTAMP -> dateTimeTextPrecise(content as Long)
            NOTATION_DURATION  -> detailedDuration(content as Long)
            NOTATION_DATA_SIZE -> getFileSizeString(content as Long)
            NOTATION_BIT_RATE  -> readableBitrate(context, content as Long)
            NOTATION_SAMPLING  -> readableSampling(context, content as Long)
            NOTATION_RAW_TEXT  -> rawText(context, content as ByteArray)
            NOTATION_BINARY    -> "<${context.getString(R.string.msg_binary)}>"
            NOTATION_EMPTY     -> "<${context.getString(R.string.msg_empty)}>"

            NOTATION_COMPOSITE -> {
                @Suppress("UNCHECKED_CAST")
                val content = content as Collection<Metadata.Field>
                content.joinToString(separator = "\n") { display(context, it) }
            }

            else               -> content.toString()
        }
    } catch (e: Exception) {
        warning(context, "Metadata", "Failed to display Field:\n$context", e)
        "<${context.getString(R.string.err_unsupported_format)}>"
    }
    return text
}

private fun limitedText(value: String): String {
    val length = value.length
    val limit = TRIM_LIMITATION * 3

    return if (length > limit) {
        "${value.take(limit)}\r\n\n...\n[$limit/$length Unicode code units]"
    } else {
        value
    }
}

private fun rawText(context: Context, value: ByteArray): String {
    if (value.isEmpty()) {
        return "<${context.getString(R.string.msg_empty)}>"
    } else if (value.size > TRIM_LIMITATION) {
        val hit = context.getString(R.string.msg_binary)
        val trimmed = String(bytes = value, offset = 0, length = TRIM_LIMITATION)
        return "[$hit?]\n$trimmed"
    } else {
        return String(value)
    }
}

private fun readableBitrate(context: Context, value: Long): String =
    if (value > 0) "$value kb/s" else context.getString(R.string.msg_unknown)

private fun readableSampling(context: Context, value: Long): String =
    if (value > 0) "$value Hz" else context.getString(R.string.msg_unknown)

private const val TRIM_LIMITATION = 1024