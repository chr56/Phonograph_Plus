/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import android.app.Activity
import android.content.Context
import java.util.*
import lib.phonograph.localization.LocalizationStore.Companion.startUpLocale

object Localization {

    /**
     * read current locale from persistence
     */
    fun currentLocale(context: Context): Locale {
        return LocalizationStore.instance(context).read(startUpLocale)
    }

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
        if (recreateActivity && context is Activity) context.recreate()
        if (saveToPersistence) saveCurrentLocale(context, newLocale)
    }

    /**
     * reset current locale in persistence
     */
    fun resetLocale(context: Context) {
        LocalizationStore.instance(context).reset()
    }

    /**
     * default locale from system
     */
    fun defaultLocale(context: Context?): Locale =
        if (context != null) context.resources.configuration.locales[0] else startUpLocale
}
