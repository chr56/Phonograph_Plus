/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.settings

import android.content.Context

/**
 * Settings that are not involved actual app logics and prerequisite to user, like version, intro, terms and condition
 */
class PrerequisiteSetting(context: Context) {

    companion object {
        //region Singleton
        private var singleton: PrerequisiteSetting? = null
        fun instance(context: Context): PrerequisiteSetting {
            if (singleton == null) singleton = PrerequisiteSetting(context.applicationContext)
            return singleton!!
        }
        //endregion
    }
}