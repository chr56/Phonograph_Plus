/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.theme

import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import android.content.DialogInterface


fun AlertDialog.tintButtons(): AlertDialog =
    apply {
        setOnShowListener {
            tintAlertDialogButtons(it as AlertDialog)
        }
    }


fun tintAlertDialogButtons(
    dialog: AlertDialog,
    @ColorInt color: Int = dialog.context.accentColor(),
) {
    dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(color)
    dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(color)
    dialog.getButton(DialogInterface.BUTTON_NEUTRAL)?.setTextColor(color)
}


@Composable
fun accentColoredButtonStyle(): TextStyle = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)