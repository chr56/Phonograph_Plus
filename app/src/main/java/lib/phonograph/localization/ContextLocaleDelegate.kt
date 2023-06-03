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
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import java.lang.IllegalStateException
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


    private var startupLocale: Locale = Locale.getDefault()
    private var firstLocaleInit = true

    private fun registerSystemLocale(context: Context) {
        if (firstLocaleInit) {
            startupLocale = context.resources.configuration.locales[0]
            firstLocaleInit = false
        }
    }

    /**
     *  read System Locale
     */
    fun systemLocale(context: Context): Locale {
        if (firstLocaleInit && SDK_INT < TIRAMISU) throw IllegalStateException("SystemLocale is unavailable since application is not initialized!")
        return if (SDK_INT >= TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
            localeManager.systemLocales[0] ?: startupLocale
        } else {
            startupLocale
        }
    }

}
