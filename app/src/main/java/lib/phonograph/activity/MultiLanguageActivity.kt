/*
 *  Copyright (c) 2022~2023 chr_56
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 */
package lib.phonograph.activity

import lib.phonograph.localization.LocalizationStore
import lib.phonograph.localization.LocalizationUtil
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle

open class MultiLanguageActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        if (VERSION.SDK_INT < TIRAMISU) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.create(LocalizationStore.current(newBase ?: this))
            )
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (VERSION.SDK_INT >= TIRAMISU) {
            LocalizationUtil.syncSystemLocale(this)
        }
    }
}
