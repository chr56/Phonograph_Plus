/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import java.util.*

object LocalizationUtil {

    var locale: Locale = Locale.getDefault()

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
        locale = target
        Locale.setDefault(target)
        // Android Context
        updateContextResources(context, target)
        if (recreateActivity && context is Activity) context.recreate()
    }

    fun updateContextResources(context: Context, newLocale: Locale) {
        val resources = context.resources
        if (resources.configuration.locales[0] == newLocale) return
        resources.updateConfiguration(
            amendConfiguration(resources.configuration, newLocale),
            resources.displayMetrics
        )
    }

    fun createNewConfigurationContext(context: Context, newLocale: Locale): Context =
        context.createConfigurationContext(
            amendConfiguration(context.resources.configuration, newLocale)
        )

    fun amendConfiguration(configuration: Configuration): Configuration =
        amendConfiguration(configuration, locale)

    private fun amendConfiguration(configuration: Configuration, newLocale: Locale): Configuration =
        configuration.apply {
            setLocale(newLocale)
            setLayoutDirection(newLocale)
        }

    @SuppressLint("ConstantLocale")
    val systemLocale: Locale = Locale.getDefault()
}
