/*
 *  Copyright (c) 2022~2024 chr_56
 */
package player.phonograph.ui.basis

import lib.phonograph.localization.LocalizationStore
import lib.phonograph.localization.LocalizationUtil
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle

open class MultiLanguageActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        if (VERSION.SDK_INT < TIRAMISU) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.create(LocalizationStore.current(newBase ?: this))
            )
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (VERSION.SDK_INT >= TIRAMISU) {
            LocalizationUtil.syncSystemLocale(this)
        }
    }
}
