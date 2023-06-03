/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import lib.phonograph.localization.LocalizationStore.Companion.startUpLocale
import androidx.annotation.RequiresApi
import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.LocaleList
import java.util.*

object Localization {

    //region Persistence
    /**
     * read current locale from persistence
     */
    fun storedLocale(context: Context): Locale {
        return LocalizationStore.instance(context).read(startUpLocale)
    }


    /**
     * save current locale to persistence
     */
    fun saveLocale(context: Context, newLocale: Locale) {
        LocalizationStore.instance(context).save(newLocale)
    }

    /**
     * reset current locale in persistence
     */
    fun resetStoredLocale(context: Context) {
        LocalizationStore.instance(context).reset()
    }
    //endregion


    /**
     * default locale from system
     */
    fun defaultLocale(): Locale = startUpLocale
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
        if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            notifySystemLocale(context, newLocale)
        }
        if (recreateActivity && context is Activity) context.recreate()
        if (saveToPersistence) saveLocale(context, newLocale)
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    private fun notifySystemLocale(context: Context, newLocale: Locale) {
        val mLocaleManager = context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
        mLocaleManager.applicationLocales = LocaleList(newLocale)
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    fun syncSystemLocale(context: Context) {
        val mLocaleManager = context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
        val newLocales = mLocaleManager.applicationLocales
        if (!newLocales.isEmpty) {
            saveLocale(context, newLocales[0])
        }
    }

}
