/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.R
import player.phonograph.repo.room.DatabaseUtil
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.domain.DatabaseSync
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DatabaseMaintenanceDialog : ComposeViewDialogFragment() {
    @Composable
    override fun Content() {
        MainContent(::dismiss)
    }

    companion object {
        fun create(): DatabaseMaintenanceDialog =
            DatabaseMaintenanceDialog().apply {
                // arguments = Bundle().apply { }
            }
    }
}

@Composable
private fun MainContent(dismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    PhonographTheme {
        MaterialDialog(
            dialogState = rememberMaterialDialogState(true),
            onCloseRequest = { dismiss() },
            buttons = {
                positiveButton(
                    res = android.R.string.ok,
                    textStyle = accentColoredButtonStyle()
                ) { dismiss() }
            }
        ) {
            Spacer(Modifier.height(12.dp))
            title(stringResource(R.string.label_database_maintenance))
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(horizontal = 12.dp)) {
                OptionItemRefresh(coroutineScope, context)
                OptionItemDelete(coroutineScope, context, dismiss)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OptionItemRefresh(coroutineScope: CoroutineScope, context: Context) {
    Option(
        stringResource(R.string.action_refresh_database),
        stringResource(R.string.tips_refresh_database)
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            DatabaseSync.checkAndRefresh(context.applicationContext, MusicDatabase.koinInstance)
        }
    }
}


@Composable
private fun OptionItemDelete(
    coroutineScope: CoroutineScope,
    context: Context,
    dismiss: () -> Unit,
) {
    Option(
        stringResource(R.string.action_delete_database),
        stringResource(R.string.tips_delete_database),
        requireConfirm = true
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            DatabaseUtil.deleteEntireDatabase(context.applicationContext, MusicDatabase.koinInstance)
            dismiss()
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Option(name: String, description: String, requireConfirm: Boolean = false, action: () -> Unit) {
    var hint by remember { mutableStateOf(false) }
    val base =
        if (requireConfirm) {
            Modifier.combinedClickable(onLongClick = action, onClick = { hint = true })
        } else {
            Modifier.combinedClickable(onClick = action)
        }

    Column(
        base
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.body1)
        Text(description, style = MaterialTheme.typography.body2)
        if (requireConfirm) {
            Text(
                stringResource(R.string.tips_long_press_to_proceed),
                style = MaterialTheme.typography.body2, color = if (hint) Color.Red else Color.LightGray
            )
        }
    }
}