/*
 * Copyright (c) 2022 chr_56
 */
package lib.phonograph.activity

import lib.phonograph.localization.ContextLocaleDelegate
import lib.phonograph.localization.Localization
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.res.Configuration
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle

open class MultiLanguageActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            ContextLocaleDelegate.attachBaseContext(newBase)
        )
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(
            ContextLocaleDelegate.onConfigurationChanged(this, newConfig)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (VERSION.SDK_INT >= TIRAMISU) {
            Localization.syncSystemLocale(this)
        }
    }
}
