/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.web

import player.phonograph.ui.compose.Navigator
import androidx.annotation.StringRes
import androidx.compose.runtime.compositionLocalOf
import android.content.Context

val LocalPageNavigator = compositionLocalOf<Navigator<Page>?> { null }

sealed class Page(@StringRes val nameRes: Int) : Navigator.IPage {
    open fun title(context: Context): String = context.getString(nameRes)
}

