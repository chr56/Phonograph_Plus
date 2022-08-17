/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.*

object LocalizationUtil {

    var currentLocale: Locale = Locale.getDefault()

    @SuppressLint("ConstantLocale")
    val systemLocale: Locale = Locale.getDefault()

    /**
     * read from persistence
     */
    fun readLocale(context: Context): Locale {
        return LocalizationStore.instance(context).read(systemLocale)
    }

    /**
     * store persistence
     */
    fun writeLocale(context: Context, newLocale: Locale) {
        LocalizationStore.instance(context).save(newLocale)
    }

    /**
     * reset to default
     */
    fun resetLocale(context: Context) {
        LocalizationStore.instance(context).reset()
    }

    /**
     * change runtime locate
     */
    fun setCurrentLocale(context: Context, target: Locale, recreateActivity: Boolean = false) {
        // Java Locale
        currentLocale = target
        Locale.setDefault(target)
        // Android Context
        updateResources(context.resources, target)
        if (recreateActivity && context is Activity) context.recreate()
    }

    @JvmOverloads
    fun updateResources(resources: Resources, newLocale: Locale = currentLocale) {
        if (resources.configuration.locales[0] == newLocale) return
        resources.updateConfiguration(
            amendConfiguration(resources.configuration, newLocale),
            resources.displayMetrics
        )
    }

    @JvmOverloads
    fun createNewConfigurationContext(context: Context, newLocale: Locale = currentLocale): Context =
        context.createConfigurationContext(
            amendConfiguration(context.resources.configuration, newLocale)
        )

    @JvmOverloads
    fun amendConfiguration(configuration: Configuration, newLocale: Locale = currentLocale): Configuration =
        configuration.apply {
            setLocale(newLocale)
            setLayoutDirection(newLocale)
        }
}
