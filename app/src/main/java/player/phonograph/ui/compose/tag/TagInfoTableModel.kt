/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import android.content.res.Resources

sealed interface TagData {
    fun text(resources: Resources): String
}

data class TextTag(val content: String) : TagData {
    override fun text(resources: Resources): String = content
}

object EmptyTag : TagData {
    override fun text(resources: Resources): String = "<Empty>"
}

object BinaryTag : TagData {
    override fun text(resources: Resources): String = "<Binary>"
}