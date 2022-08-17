/*
 * Copyright (c) 2022 chr_56
 */
package lib.phonograph.activity

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import lib.phonograph.localization.ContextLocaleDelegate

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
}
