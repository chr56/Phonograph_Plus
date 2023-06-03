/*
 * Copyright (c) 2022 chr_56
 */

package lib.phonograph.localization

import androidx.annotation.RequiresApi
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.*

object LocalizationUtil {
    @JvmOverloads
    fun updateResources(
        resources: Resources,
        newLocale: Locale = Locale.getDefault(),
    ) {
        resources.updateConfiguration(
            amendConfiguration(resources.configuration, newLocale),
            resources.displayMetrics
        )
    }

    @JvmOverloads
    fun createNewConfigurationContext(
        context: Context,
        newLocale: Locale = LocalizationStore.current(context),
    ): Context =
        context.createConfigurationContext(
            amendConfiguration(context.resources.configuration, newLocale)
        )

    @JvmOverloads
    fun amendConfiguration(
        configuration: Configuration,
        newLocale: Locale = Locale.getDefault(),
    ): Configuration =
        configuration.apply {
            setLocale(newLocale)
            setLayoutDirection(newLocale)
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun syncSystemLocale(context: Context) {
        val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
        val newLocales = localeManager.applicationLocales
        if (!newLocales.isEmpty) {
            LocalizationStore.save(context, newLocales[0])
        } else {

        }
    }
}