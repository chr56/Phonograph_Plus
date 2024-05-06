/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.theme

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import player.phonograph.settings.ThemeSetting
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import android.content.DialogInterface


fun MaterialDialog.tintButtons(): MaterialDialog = tintButtons(ThemeSetting.accentColor(context))

fun MaterialDialog.tintButtons(@ColorInt color: Int): MaterialDialog {
    getActionButton(WhichButton.POSITIVE).updateTextColor(color)
    getActionButton(WhichButton.NEGATIVE).updateTextColor(color)
    getActionButton(WhichButton.NEUTRAL).updateTextColor(color)
    return this
}

fun AlertDialog.tintButtons(): AlertDialog =
    apply {
        setOnShowListener {
            tintAlertDialogButtons(it as AlertDialog)
        }
    }


fun tintAlertDialogButtons(
    dialog: AlertDialog,
    @ColorInt color: Int = ThemeSetting.accentColor(dialog.context),
) {
    dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(color)
    dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(color)
    dialog.getButton(DialogInterface.BUTTON_NEUTRAL)?.setTextColor(color)
}


@Composable
fun accentColoredButtonStyle(): TextStyle = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.secondary)