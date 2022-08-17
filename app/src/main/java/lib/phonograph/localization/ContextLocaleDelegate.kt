/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import android.content.ComponentCallbacks
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration

object ContextLocaleDelegate {

    /**
     * wrap [newBase] with this in [ContextWrapper.attachBaseContext]
     */
    fun attachBaseContext(newBase: Context?): Context? =
        if (newBase != null) {
            LocalizationUtil.createNewConfigurationContext(
                context = newBase,
                newLocale = LocalizationUtil.readLocale(newBase)
            )
        } else {
            newBase
        }

    /**
     * Wrap [newConfig] with this in [ComponentCallbacks.onConfigurationChanged]
     */
    fun onConfigurationChanged(newConfig: Configuration): Configuration =
        LocalizationUtil.amendConfiguration(newConfig)
}
