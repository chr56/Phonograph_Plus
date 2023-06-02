/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import android.app.Activity
import android.content.Context
import java.util.*
import lib.phonograph.localization.LocalizationStore.Companion.startUpLocale
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import android.os.Build
import android.os.Build.VERSION_CODES

object Localization {

    /**
     * read current locale from persistence
     */
    fun currentLocale(context: Context): Locale {
        return LocalizationStore.instance(context).read(startUpLocale)
    }

    /**
     * default locale from system
     */
    fun defaultLocale(): Locale = startUpLocale

    /**
     * save current locale to persistence
     */
    fun saveCurrentLocale(context: Context, newLocale: Locale) {
        LocalizationStore.instance(context).save(newLocale)
    }

    /**
     * change runtime locate
     */
    fun setCurrentLocale(
        context: Context,
        newLocale: Locale,
        recreateActivity: Boolean = false,
        saveToPersistence: Boolean = true
    ) {
        Locale.setDefault(newLocale)
        LocalizationUtil.updateResources(context.resources, newLocale)
        if (context is AppCompatActivity) {
            if (Build.VERSION.SDK_INT < VERSION_CODES.TIRAMISU) {
                val localeListCompat = LocaleListCompat.create(newLocale)
                AppCompatDelegate.setApplicationLocales(localeListCompat)
            } else {
                if (context.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    val localeListCompat = LocaleListCompat.create(newLocale)
                    AppCompatDelegate.setApplicationLocales(localeListCompat)
                }
            }
        }
        if (recreateActivity && context is Activity) context.recreate()
        if (saveToPersistence) saveCurrentLocale(context, newLocale)
    }

    /**
     * reset current locale in persistence
     */
    fun resetLocale(context: Context) {
        LocalizationStore.instance(context).reset()
    }
}
