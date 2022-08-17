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

    fun save(locale: Locale) {
        val language = locale.language
        val region = locale.country
        preference.edit().also { editor ->
            editor.putString(LANGUAGE, language)
            editor.putString(REGION, region)
            editor.apply()
        }
    }
    fun read(default: Locale): Locale {
        val language = preference.getString(LANGUAGE, null)
        val region = preference.getString(REGION, null)
        val locale = if (language != null) {
            if (region != null) Locale(language, region) else Locale(language)
        } else default
        return locale
    }

    fun reset() {
        preference.edit().also { editor ->
            editor.remove(LANGUAGE)
            editor.remove(REGION)
            editor.apply()
        }
    }

    fun shutDown() {
        sInstance = null
    }

    companion object {
        const val FILE_NAME = "Locale"

        private var sInstance: LocalizationStore? = null
        fun instance(context: Context): LocalizationStore {
            return sInstance ?: LocalizationStore(context).apply { sInstance = this }
        }

        private const val LANGUAGE = "language"
        private const val REGION = "region"
    }
}
