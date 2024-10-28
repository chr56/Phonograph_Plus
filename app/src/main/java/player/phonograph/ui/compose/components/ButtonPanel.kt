/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ButtonPanel(
    cancelText: String,
    cancel: () -> Unit,
    confirmText: String,
    confirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        TextButton(cancel) {
            Text(
                cancelText,
                style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(confirm) {
            Text(
                confirmText,
                style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
            )
        }
    }
}

@Composable
fun ButtonPanel(
    text: String,
    callback: () -> Unit,
    modifier: Modifier = Modifier,
    left: Boolean = false,
) {
    Row(modifier) {
        if (!left) Spacer(Modifier.weight(1f))
        TextButton(callback) {
            Text(
                text,
                style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)
            )
        }
        if (left) Spacer(Modifier.weight(1f))
    }
}