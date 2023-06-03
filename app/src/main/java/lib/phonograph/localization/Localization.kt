/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import android.content.Context
import java.util.Locale

object Localization {

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

    fun clearStoredLocale(context: Context) {
        LocalizationStore.instance(context).clear()
    }

}
