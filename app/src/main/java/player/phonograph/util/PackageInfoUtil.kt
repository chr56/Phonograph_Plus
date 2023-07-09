/*
 *  Copyright (c) 2023 chr_56
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

package player.phonograph.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU

private const val NA = "Unknown"


private fun getPackageInfo(context: Context, flags: Int): PackageInfo? {
    return try {
        val packageManager = context.packageManager
        if (SDK_INT > TIRAMISU) {
            packageManager.getPackageInfo(context.packageName, PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, flags)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        return null
    }
}

fun gitRevisionHash(context: Context): String {
    val packageInfo = getPackageInfo(context, PackageManager.GET_META_DATA) ?: return NA
    return packageInfo.applicationInfo.metaData.getString("GitCommitHash") ?: NA
}

fun currentVersionName(context: Context): String {
    val packageInfo = getPackageInfo(context, 0) ?: return NA
    return packageInfo.versionName
}

fun currentVersionCode(context: Context): Int {
    val packageInfo = getPackageInfo(context, 0) ?: return -1
    @Suppress("DEPRECATION")
    return packageInfo.versionCode
}