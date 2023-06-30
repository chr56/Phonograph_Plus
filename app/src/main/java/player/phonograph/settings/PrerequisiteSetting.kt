/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import player.phonograph.PACKAGE_NAME
import android.content.Context
import android.content.SharedPreferences

/**
 * Settings that are not involved actual app logics and prerequisite to user, like version, intro, terms and condition
 */
class PrerequisiteSetting(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE)

    var introShown: Boolean
        get() = sharedPreferences.getBoolean(INTRO_SHOWN, false)
        set(value) {
            sharedPreferences.edit().putBoolean(INTRO_SHOWN, value).apply()
        }

    companion object {
        const val INTRO_SHOWN = "intro_shown"
        //region Singleton
        private var singleton: PrerequisiteSetting? = null
        fun instance(context: Context): PrerequisiteSetting {
            if (singleton == null) singleton = PrerequisiteSetting(context.applicationContext)
            return singleton!!
        }
        //endregion
    }
}