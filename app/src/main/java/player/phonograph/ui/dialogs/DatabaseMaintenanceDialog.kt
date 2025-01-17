/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import player.phonograph.ui.compose.ComposeViewDialogFragment
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.util.theme.accentColoredButtonStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
            title("Internal Database Maintenance")
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
        "Refresh Database",
        "Scan songs and index them"
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            // Stub
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
        "Delete Database",
        "Delete entire database, including all Database Playlists and indexed songs!"
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            // Stub
            dismiss()
        }
    }
}

@Composable
private fun Option(name: String, description: String, action: () -> Unit) {
    Column(
        Modifier
            .clickable { action() }
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.body1)
        Text(description, style = MaterialTheme.typography.body2)
    }
}