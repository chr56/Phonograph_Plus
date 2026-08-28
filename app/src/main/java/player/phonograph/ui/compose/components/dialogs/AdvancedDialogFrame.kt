/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose.components.dialogs

import player.phonograph.R
import player.phonograph.model.menu.ActionEntry
import player.phonograph.ui.compose.components.actions.ActionIconButton
import player.phonograph.ui.compose.components.actions.CollapsedActionMenu
import player.phonograph.ui.resource.ConcreteActionSemantic
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource


/**
 * Basic Frame for advanced or complex dialogs that feel likes a page, in full screen
 */
@Composable
fun AdvancedDialogFrame(
    modifier: Modifier,
    title: String,
    onDismissRequest: () -> Unit,
    navigationButtonIcon: Painter? = rememberVectorPainter(Icons.AutoMirrored.Default.ArrowBack),
    actions: List<ActionEntry> = emptyList(),
    collapsedActions: List<ActionEntry> = emptyList(),
    iconsInCollapsedMenu: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        TopAppBar(
            title = { Text(title) },
            modifier = Modifier.fillMaxWidth(),
            navigationIcon = navigationIcon(navigationButtonIcon, onDismissRequest),
            actions = {
                for (item in actions) ActionIconButton(item)
                if (collapsedActions.isNotEmpty()) CollapsedActionMenu(collapsedActions, iconsInCollapsedMenu)
            }
        )
        content()
    }
}

interface DialogBuilderScope {

    fun title(text: String)

    fun navigationButton(painter: Painter?)

    fun actionButton(
        imageVector: ImageVector? = null,
        imageRes: Int = -1,
        text: String? = null,
        textRes: Int = -1,
        onClick: () -> Unit,
    )

    fun actionButton(item: ActionEntry)

    fun actionButtons(items: List<ActionEntry>)

    fun content(block: @Composable ColumnScope.() -> Unit)

    fun onDismissRequest(block: () -> Unit)

}

/**
 * Basic Frame for advanced or complex dialogs that feel likes a page, in full screen
 */
@Composable
fun AdvancedDialogFrame(
    modifier: Modifier,
    specification: @Composable DialogBuilderScope.() -> Unit,
) {
    DialogBuilder().apply {
        specification()
        Build(modifier)
    }
}

private class DialogBuilder : DialogBuilderScope {

    private var _title: String = ""
    private var _painter: Painter? = null
    private val _actionButtons: MutableList<ActionEntry> = mutableListOf()
    private var _content: @Composable (ColumnScope.() -> Unit) = {}
    private var _onDismissRequest: () -> Unit = {}

    override fun title(text: String) {
        _title = text
    }

    override fun navigationButton(painter: Painter?) {
        _painter = painter
    }

    override fun actionButton(
        imageVector: ImageVector?,
        imageRes: Int,
        text: String?,
        textRes: Int,
        onClick: () -> Unit,
    ) {
        _actionButtons.add(
            ActionEntry.ToolbarEntry(
                semantic = ConcreteActionSemantic(
                    imageVector = imageVector,
                    imageRes = imageRes,
                    text = text,
                    textRes = textRes,
                ),
                action = onClick,
            )
        )
    }

    override fun actionButton(item: ActionEntry) {
        _actionButtons.add(item)
    }

    override fun actionButtons(items: List<ActionEntry>) {
        _actionButtons.addAll(items)
    }

    override fun content(block: @Composable (ColumnScope.() -> Unit)) {
        _content = block
    }

    override fun onDismissRequest(block: () -> Unit) {
        _onDismissRequest = block
    }

    @Composable
    fun Build(modifier: Modifier) {
        AdvancedDialogFrame(
            modifier = modifier,
            title = _title,
            onDismissRequest = _onDismissRequest,
            navigationButtonIcon = _painter,
            actions = _actionButtons,
            content = _content,
        )
    }
}


@Composable
private fun navigationIcon(painter: Painter?, onClick: () -> Unit) =
    if (painter != null) {
        @Composable {
            ActionIconButton(
                painter,
                tint = MaterialTheme.colors.onPrimary,
                text = stringResource(R.string.action_exit),
                onClick = onClick
            )
        }
    } else {
        null
    }

