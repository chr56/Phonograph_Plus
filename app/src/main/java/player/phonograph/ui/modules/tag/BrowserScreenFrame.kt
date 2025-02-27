/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.modules.tag.components.MetadataDifferenceItem
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp


@Composable
fun <VM : AbsMetadataViewModel> BrowserScreenFrame(
    viewModel: VM,
    fileListSection: (@Composable (ColumnScope.() -> Unit))?,
    artworkSection: @Composable (ColumnScope.() -> Unit),
    block: @Composable (ColumnScope.() -> Unit),
) {
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize(),
    ) {

        if (fileListSection != null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Title(stringResource(R.string.files), color = MaterialTheme.colors.primary)
                fileListSection(this)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        artworkSection(this)
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            block(this)
        }
        Spacer(modifier = Modifier.height(64.dp))
    }
    val editable by viewModel.editable.collectAsState()
    if (editable) {
        val context = LocalContext.current
        SaveConfirmationDialog(
            viewModel.saveConfirmationDialogState,
            viewModel::generateMetadataDifference
        ) { viewModel.submitEvent(context, MetadataUIEvent.Save) }
        val activity = LocalActivity.current
        ExitWithoutSavingDialog(viewModel.exitWithoutSavingDialogState) { activity?.finish() }
    }
}


@Composable
fun SaveConfirmationDialog(
    dialogState: MaterialDialogState,
    changes: () -> MetadataChanges,
    onSave: () -> Unit,
) {
    val dismiss = { dialogState.hide() }
    val save = {
        dismiss()
        onSave()
    }
    MaterialDialog(
        dialogState = dialogState,
        elevation = 0.dp,
        autoDismiss = false,
        buttons = {
            button(res = R.string.save, onClick = save, textStyle = accentColoredButtonStyle())
            button(res = android.R.string.cancel, onClick = dismiss, textStyle = accentColoredButtonStyle())
        }
    ) {
        title(res = R.string.save)
        customView {
            MetadataDifferenceScreen(changes = changes())
        }
    }
}


@Composable
private fun MetadataDifferenceScreen(changes: MetadataChanges) {
    if (changes.changes.isEmpty()) {
        Text(text = stringResource(id = R.string.no_changes))
    } else {
        LazyColumn(Modifier.padding(8.dp)) {
            for (change in changes.changes) {
                item {
                    MetadataDifferenceItem(change.first, change.second)
                }
            }
        }
    }
}


@Composable
private fun ExitWithoutSavingDialog(
    dialogState: MaterialDialogState,
    onExit: () -> Unit,
) {
    MaterialDialog(
        dialogState = dialogState,
        elevation = 0.dp,
        autoDismiss = false,
        buttons = {
            positiveButton(
                res = android.R.string.cancel,
                onClick = { dialogState.hide() },
                textStyle = accentColoredButtonStyle()
            )
            button(res = android.R.string.ok, textStyle = accentColoredButtonStyle()) {
                dialogState.hide()
                onExit()
            }
        }
    ) {
        title(res = R.string.exit_without_saving)
    }
}