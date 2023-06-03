/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.app.LocaleManager
import android.content.Context
import android.os.Build.VERSION_CODES
import android.os.LocaleList
import java.util.*

object Localization {

    //region Persistence
    /**
     * read current locale from persistence
     */
    fun storedLocale(context: Context): Locale {
        return LocalizationStore.instance(context).read()
    }


    /**
     * save current locale to persistence
     */
    fun saveLocale(context: Context, newLocale: Locale) {
        LocalizationStore.instance(context).save(newLocale)
    }

    //endregion


    /**
     * change runtime locate
     */
    fun modifyLocale(
        context: Context,
        newLocales: LocaleListCompat,
    ) {
        AppCompatDelegate.setApplicationLocales(newLocales)
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
