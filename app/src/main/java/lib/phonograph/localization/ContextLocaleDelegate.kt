/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import lib.phonograph.localization.LocalizationUtil.amendConfiguration
import lib.phonograph.localization.LocalizationUtil.createNewConfigurationContext
import android.app.LocaleManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object ContextLocaleDelegate {

    /**
     * wrap [newBase] with this in [ContextWrapper.attachBaseContext]
     */
    fun attachBaseContext(newBase: Context?): Context? =
        if (newBase != null) {
            registerSystemLocale(newBase)
            createNewConfigurationContext(
                context = newBase,
                newLocale = LocalizationStore.current(newBase)
            )
        } else {
            newBase
        }

    /**
     * Wrap [newConfig] with this in [ComponentCallbacks.onConfigurationChanged]
     */
    fun onConfigurationChanged(context: Context, newConfig: Configuration): Configuration =
        amendConfiguration(newConfig, LocalizationStore.current(context))


    var startupLocale: Locale = Locale.getDefault()
        private set
    private var firstInit = true

    private fun registerSystemLocale(context: Context) {
        if (firstInit) {
            startupLocale = systemLocale(context)
            firstInit = false
        }
    }

    /**
     *  read System Locale
     */
    private fun systemLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            context.resources.configuration.locales[0]
        } else {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
            localeManager.systemLocales[0]
        }
    }

}
