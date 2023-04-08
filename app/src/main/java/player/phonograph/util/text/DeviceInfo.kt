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

package player.phonograph.util.text

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.IntRange
import player.phonograph.BuildConfig
import java.util.*

@SuppressLint("ObsoleteSdkInt")
fun getDeviceInfo(context: Context): String {

    // App
    val pm: PackageManager = context.packageManager
    val packageInfo: PackageInfo? = pm.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)

    var packageName: String = "null"
    var versionName: String = "null"
    var versionCode: String = "null"
    packageInfo?.let {
        versionName = packageInfo.versionName
        packageName = packageInfo.packageName
        versionCode = packageInfo.versionCode.toString()
    }
    val gitCommitHash: String = BuildConfig.GIT_COMMIT_HASH.substring(0, 20)
    val appLanguage: String = Locale.getDefault().language

    // os
    val releaseVersion = Build.VERSION.RELEASE
    @IntRange(from = 0)
    val sdkVersion: Int = Build.VERSION.SDK_INT
    val buildID: String = Build.DISPLAY
    val buildVersion = Build.VERSION.INCREMENTAL
    // device
    val brand: String = Build.BRAND
    val manufacturer: String = Build.MANUFACTURER
    val model: String = Build.MODEL
    val device: String = Build.DEVICE // device code name
    val product: String = Build.PRODUCT // rom code name
    val hardware: String = Build.HARDWARE // motherboard?

    return """
            Package name:    $packageName
            App version:     $versionName ($versionCode)
            Git Commit Hash: $gitCommitHash
            Android version: $releaseVersion (SDK $sdkVersion)
            Device brand:    $brand  (by $manufacturer)
            Device model:    $model (code: $device)
            Product name:    $product
            Build version:   $buildID 
                             ($buildVersion)
            Hardware:        $hardware
            Language:        $appLanguage

            """.trimIndent()
}