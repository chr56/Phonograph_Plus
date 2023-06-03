/*
 * Copyright (c) 2022 chr_56
 */
package lib.phonograph.activity

import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.LocaleListCompat
import android.os.Bundle

open class MultiLanguageActivity : AppCompatActivity() {

    // override fun attachBaseContext(newBase: Context?) {
    //     super.attachBaseContext(
    //         ContextLocaleDelegate.attachBaseContext(newBase)
    //     )
    // }
    //
    // override fun onConfigurationChanged(newConfig: Configuration) {
    //     super.onConfigurationChanged(
    //         ContextLocaleDelegate.onConfigurationChanged(this, newConfig)
    //     )
    // }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(Localization.storedLocale(this)))
    }

    override fun onLocalesChanged(locales: LocaleListCompat) {
        super.onLocalesChanged(locales)
    }
}
