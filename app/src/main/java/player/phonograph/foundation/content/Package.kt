/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.foundation.content

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES


object PackageMetadata {

    fun packageInfo(context: Context, packageName: String, flags: Int): PackageInfo? {
        return try {
            val packageManager = context.packageManager
            if (SDK_INT > VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, flags)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return null
        }
    }

    fun applicationInfo(context: Context, packageName: String = context.packageName): ApplicationInfo? {
        val packageInfo = packageInfo(context, packageName, PackageManager.GET_META_DATA) ?: return null
        return packageInfo.applicationInfo
    }

    fun versionName(context: Context, packageName: String = context.packageName): String? {
        val packageInfo = packageInfo(context, packageName, 0) ?: return null
        return packageInfo.versionName
    }

    fun versionCode(context: Context, packageName: String = context.packageName): Int {
        val packageInfo = packageInfo(context, packageName, 0) ?: return -1
        @Suppress("DEPRECATION")
        return packageInfo.versionCode
    }

    fun metadata(context: Context, packageName: String = context.packageName, key: String): String? {
        val applicationInfo = applicationInfo(context, packageName) ?: return null
        return applicationInfo.metaData.getString(key)
    }

    fun signatures(context: Context, packageName: String = context.packageName): Array<Signature>? {
        if (SDK_INT > VERSION_CODES.P) {
            val packageInfo = packageInfo(context, packageName, PackageManager.GET_SIGNING_CERTIFICATES) ?: return null
            return packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            val packageInfo = packageInfo(context, packageName, PackageManager.GET_SIGNATURES) ?: return null
            @Suppress("DEPRECATION")
            return packageInfo.signatures
        }
    }

    const val METADATA_KEY_GIT_COMMIT = "GitCommitHash"
}


