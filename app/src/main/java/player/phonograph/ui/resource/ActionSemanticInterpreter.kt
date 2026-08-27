/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.resource

import player.phonograph.model.menu.ActionSemantic
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.core.content.res.ResourcesCompat
import android.content.res.Resources
import android.graphics.drawable.Drawable

interface ActionSemanticInterpreter {
    fun text(resources: Resources): String

    fun icon(resources: Resources): Drawable?

    fun icon(): ImageVector?
}

@Composable
fun ActionSemanticInterpreter.text(): String = text(LocalResources.current)

@Composable
fun ActionSemanticInterpreter.painter(): Painter {
    val imageVector = icon()
    return if (imageVector != null) {
        rememberVectorPainter(imageVector)
    } else {
        painterResource((this as ConcreteActionSemantic).imageRes)
    }
}


data class ConcreteActionSemantic(
    val textRes: Int = -1,
    val text: String? = null,
    val imageRes: Int = -1,
    val imageVector: ImageVector? = null,
) : ActionSemantic, ActionSemanticInterpreter {

    override fun text(resources: Resources): String = text ?: resources.getString(textRes)

    override fun icon(resources: Resources): Drawable? = ResourcesCompat.getDrawable(resources, imageRes, null)

    override fun icon(): ImageVector? = imageVector

}

fun ActionSemantic.interpreter(): ActionSemanticInterpreter = this as ConcreteActionSemantic
