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

import player.phonograph.BuildConfig
import player.phonograph.util.currentVersionCode
import player.phonograph.util.currentVersionName
import player.phonograph.util.gitRevisionHash
import player.phonograph.util.permissions.hasStorageReadPermission
import player.phonograph.util.permissions.hasStorageWritePermission
import androidx.annotation.IntRange
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import java.util.Locale

@SuppressLint("ObsoleteSdkInt")
fun getDeviceInfo(context: Context): String {

    // App

    val versionName: String = currentVersionName(context)
    val versionCode: String = currentVersionCode(context).toString()
    val packageName: String = context.packageName
    val gitCommitHash: String = gitRevisionHash(context)
    val favor: String = BuildConfig.FLAVOR
    val storage: String = storagePermissionInfo(context)

    // os
    val releaseVersion = Build.VERSION.RELEASE
    val sdkVersion: Int = Build.VERSION.SDK_INT
    val buildID: String = Build.DISPLAY
    val buildVersion = Build.VERSION.INCREMENTAL
    // device
    val arch: String = Build.SUPPORTED_ABIS.joinToString()
    val soc: String = socInfo()
    val brand: String = Build.BRAND
    val manufacturer: String = Build.MANUFACTURER
    val model: String = Build.MODEL
    val device: String = Build.DEVICE // device code name
    val product: String = Build.PRODUCT // rom code name
    val appLanguage: String = Locale.getDefault().language
    val screenInfo = screenInfo(context.resources.displayMetrics)

    return """
            App version:     $versionName ($versionCode)
            Git Commit Hash: $gitCommitHash
            Package name:    $packageName
            Release favor:   $favor
            Android version: $releaseVersion (API $sdkVersion)
            Architecture:    $arch
            Soc:             $soc 
            Device brand:    $brand (by $manufacturer)
            Device model:    $product/$model (code $device)
            Build version:   $buildID
                             ($buildVersion)
            Language:        $appLanguage
            Screen:          $screenInfo
            Permissions:     Storage($storage)

            """.trimIndent()
}

private fun socInfo(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    "${Build.SOC_MODEL}/${Build.BOARD} (by ${Build.SOC_MANUFACTURER})"
} else {
    "${Build.HARDWARE}/${Build.BOARD}"
}

private fun screenInfo(displayMetrics: DisplayMetrics): String =
    "${displayMetrics.heightPixels}x${displayMetrics.widthPixels} (dpi ${displayMetrics.densityDpi})"

private fun storagePermissionInfo(context: Context): String {
    return buildString {
        if (hasStorageReadPermission(context)) {
            append("READ")
        }
        if (hasStorageReadPermission(context)) {
            append(" WRITE")
        }
    }
}