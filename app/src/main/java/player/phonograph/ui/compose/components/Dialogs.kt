/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.compose.components

import player.phonograph.R
import player.phonograph.ui.compose.dialogHorizontalPadding
import player.phonograph.ui.compose.dialogMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.DropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties


@Composable
fun LimitedDialog(
    elevation: Dp = 24.dp,
    backgroundColor: Color = MaterialTheme.colors.surface,
    properties: DialogProperties = DialogProperties(),
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val containerSize = LocalWindowInfo.current
    val maxHeight = dialogMaxHeight(containerSize)
    val horizontalPadding = dialogHorizontalPadding(containerSize)
    Dialog(
        properties = properties,
        onDismissRequest = { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(maxHeight = maxHeight)
                .padding(horizontal = horizontalPadding)
                .clipToBounds()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            color = backgroundColor,
            elevation = elevation
        ) {
            content()
        }
    }
}


/**
 * Basic Frame for advanced or complex dialogs that feel likes a page, in full screen
 */
@Composable
fun AdvancedDialogFrame(
    modifier: Modifier,
    title: String,
    onDismissRequest: () -> Unit,
    navigationButtonIcon: Painter? = rememberVectorPainter(Icons.AutoMirrored.Default.ArrowBack),
    actions: List<ActionItem> = emptyList(),
    collapsedActions: List<ActionItem> = emptyList(),
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
        tint: Color? = null,
        onClick: () -> Unit,
    )

    fun actionButton(item: ActionItem)

    fun actionButtons(items: List<ActionItem>)

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

private class DialogBuilder() : DialogBuilderScope {

    private var _title: String = ""
    private var _painter: Painter? = null
    private val _actionButtons: MutableList<ActionItem> = mutableListOf()
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
        tint: Color?,
        onClick: () -> Unit,
    ) {
        _actionButtons.add(
            ActionItem(
                imageVector = imageVector,
                imageRes = imageRes,
                text = text,
                textRes = textRes,
                tint = tint,
                onClick = onClick
            )
        )
    }

    override fun actionButton(item: ActionItem) {
        _actionButtons.add(item)
    }

    override fun actionButtons(items: List<ActionItem>) {
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
        Column(modifier) {
            TopAppBar(
                title = { Text(_title) },
                modifier = Modifier.fillMaxWidth(),
                navigationIcon = navigationIcon(_painter, _onDismissRequest),
                actions = { for (item in _actionButtons) ActionIconButton(item) }
            )
            _content()
        }
    }
}


@Composable
private fun navigationIcon(painter: Painter?, onClick: () -> Unit) =
    optionalActionIconButton(
        painter,
        tint = MaterialTheme.colors.onPrimary,
        text = stringResource(R.string.action_exit),
        onClick = onClick
    )


@Composable
fun CollapsedActionMenu(actions: List<ActionItem>, withIcon: Boolean, modifier: Modifier = Modifier) {
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier) {
        ActionIconButton(
            ActionItem(imageVector = Icons.Default.MoreVert, textRes = R.string.action_more) { showMenu = true }
        )
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropDownMenuContent(actions, withIcon = withIcon)
        }
    }
}