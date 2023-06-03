/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.*

object LocalizationUtil {
    @JvmOverloads
    fun updateResources(
        resources: Resources,
        newLocale: Locale = Locale.getDefault()
    ) {
        resources.updateConfiguration(
            amendConfiguration(resources.configuration, newLocale),
            resources.displayMetrics
        )
    }

    @JvmOverloads
    fun createNewConfigurationContext(
        context: Context,
        newLocale: Locale = Localization.storedLocale(context)
    ): Context =
        context.createConfigurationContext(
            amendConfiguration(context.resources.configuration, newLocale)
        )

    @JvmOverloads
    fun amendConfiguration(
        configuration: Configuration,
        newLocale: Locale = Locale.getDefault()
    ): Configuration =
        configuration.apply {
            setLocale(newLocale)
            setLayoutDirection(newLocale)
        }
}