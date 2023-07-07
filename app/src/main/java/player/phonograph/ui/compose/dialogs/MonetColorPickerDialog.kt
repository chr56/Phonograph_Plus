/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.dialogs

import lib.phonograph.misc.ColorPalette.MODE_MONET_ACCENT_COLOR
import lib.phonograph.misc.ColorPalette.MODE_MONET_PRIMARY_COLOR
import mt.pref.ThemeColor
import player.phonograph.ui.compose.components.MonetColorPicker
import player.phonograph.util.lifecycleScopeOrNewOne
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun MonetColorPickerDialogContent(
    mode: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    MonetColorPicker { type: Int, depth: Int ->
        context.lifecycleScopeOrNewOne().launch {
            ThemeColor.edit(context) {
                when (mode) {
                    MODE_MONET_PRIMARY_COLOR -> preferredMonetPrimaryColor(type, depth)
                    MODE_MONET_ACCENT_COLOR  -> preferredMonetAccentColor(type, depth)
                }
            }
            onDismiss()
        }
    }
}