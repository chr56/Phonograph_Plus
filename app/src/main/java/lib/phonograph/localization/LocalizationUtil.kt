/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import java.util.*

object LocalizationUtil {

    var locale: Locale = Locale.getDefault()

    /**
     * read from persistence & change runtime locate
     */
    fun readLocale(context: Context, recreateActivity: Boolean = false) {
        val pref = LocalizationStore.instance(context).read(systemLocale)
        adjustCurrentLocale(context, pref, recreateActivity)
    }

    /**
     * change runtime locate & store persistence
     */
    fun writeLocale(context: Context, newLocale: Locale, recreateActivity: Boolean = false) {
        adjustCurrentLocale(context, newLocale, recreateActivity)
        // Persistence
        LocalizationStore.instance(context).save(newLocale)
    }

    /**
     * change runtime locate
     */
    fun adjustCurrentLocale(context: Context, target: Locale, recreateActivity: Boolean = false) {
        // Java Locale
        locale = target
        Locale.setDefault(target)
        // Android Context
        updateContextResources(context, target)
        if (recreateActivity && context is Activity) context.recreate()
    }

    /**
     * reset to default & clear persistence
     */
    fun resetLocale(context: Context, recreateActivity: Boolean = false) {
        adjustCurrentLocale(context, systemLocale, recreateActivity)
        LocalizationStore.instance(context).reset()
    }

    private fun updateContextResources(context: Context, newLocale: Locale) {
        val resources = context.resources
        if (resources.configuration.locales[0] == newLocale) {
            return
        }
        val configuration = resources.configuration.apply {
            setLocale(newLocale)
            setLayoutDirection(newLocale)
        }
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    @SuppressLint("ConstantLocale")
    val systemLocale: Locale = Locale.getDefault()
}
