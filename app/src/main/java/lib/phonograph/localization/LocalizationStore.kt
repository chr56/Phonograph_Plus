/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import android.content.Context
import android.content.SharedPreferences
import java.util.*

class LocalizationStore private constructor(context: Context) {

    val preference: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private var localeCache: Locale? = null

    fun save(locale: Locale) {
        localeCache = locale
        preference.edit().also { editor ->
            editor.putString(KEY_LANGUAGE, locale.language)
            editor.putString(KEY_REGION, locale.country)
            editor.apply()
        }
    }

    fun read(): Locale {
        val cache = localeCache
        if (cache != null) {
            return cache
        } else {
            val language = preference.getString(KEY_LANGUAGE, null)
            val region = preference.getString(KEY_REGION, null)
            val locale = parseLocale(language, region) ?: Locale.getDefault()
            localeCache = locale
            return locale
        }
    }

    fun clear() {
        preference.edit().also { editor ->
            editor.remove(KEY_LANGUAGE)
            editor.remove(KEY_REGION)
            editor.apply()
        }
        localeCache = null
    }

    fun shutDown() {
        sInstance = null
    }

    private fun parseLocale(language: String?, region: String?): Locale? =
        if (language != null) {
            if (region != null) Locale(language, region) else Locale(language)
        } else {
            null
        }

    companion object {
        const val FILE_NAME = "Locale"

        private var sInstance: LocalizationStore? = null
        fun instance(context: Context): LocalizationStore {
            return sInstance ?: LocalizationStore(context).apply { sInstance = this }
        }

        private const val KEY_LANGUAGE = "language"
        private const val KEY_REGION = "region"

    }
}
