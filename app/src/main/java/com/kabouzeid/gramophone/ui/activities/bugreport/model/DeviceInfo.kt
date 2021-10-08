package com.kabouzeid.gramophone.ui.activities.bugreport.model

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.IntRange
import com.kabouzeid.gramophone.BuildConfig
import java.util.Arrays

class DeviceInfo(context: Context) {
    private var versionCode = 0
    private var versionName: String? = null
    private val buildVersion = Build.VERSION.INCREMENTAL
    private val releaseVersion = Build.VERSION.RELEASE
    private val packageName = BuildConfig.APPLICATION_ID

    @IntRange(from = 0)
    private val sdkVersion = Build.VERSION.SDK_INT
    private val buildID = Build.DISPLAY
    private val brand = Build.BRAND
    private val manufacturer = Build.MANUFACTURER
    private val device = Build.DEVICE
    private val model = Build.MODEL
    private val product = Build.PRODUCT
    private val hardware = Build.HARDWARE

    @SuppressLint("NewApi")
    private val abis =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) Build.SUPPORTED_ABIS else arrayOf(
            Build.CPU_ABI,
            Build.CPU_ABI2
        )

    @SuppressLint("NewApi")
    private val abis32Bits =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) Build.SUPPORTED_32_BIT_ABIS else null

    @SuppressLint("NewApi")
    private val abis64Bits =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) Build.SUPPORTED_64_BIT_ABIS else null

    fun toMarkdown(): String {
        return """
               Device info:
               ---
               <table>
               <tr><td>Package name</td><td>$packageName</td></tr>
               <tr><td>App version</td><td>$versionName</td></tr>
               <tr><td>App version code</td><td>$versionCode</td></tr>
               <tr><td>Android build version</td><td>$buildVersion</td></tr>
               <tr><td>Android release version</td><td>$releaseVersion</td></tr>
               <tr><td>Android SDK version</td><td>$sdkVersion</td></tr>
               <tr><td>Android build ID</td><td>$buildID</td></tr>
               <tr><td>Device brand</td><td>$brand</td></tr>
               <tr><td>Device manufacturer</td><td>$manufacturer</td></tr>
               <tr><td>Device name</td><td>$device</td></tr>
               <tr><td>Device model</td><td>$model</td></tr>
               <tr><td>Device product name</td><td>$product</td></tr>
               <tr><td>Device hardware name</td><td>$hardware</td></tr>
               <tr><td>ABIs</td><td>${Arrays.toString(abis)}</td></tr>
               <tr><td>ABIs (32bit)</td><td>${Arrays.toString(abis32Bits)}</td></tr>
               <tr><td>ABIs (64bit)</td><td>${Arrays.toString(abis64Bits)}</td></tr>
               </table>
               
        """.trimIndent()
    }

    override fun toString(): String {
        return """
            Package name: $packageName
            App version: $versionName
            App version code: $versionCode
            Android build version: $buildVersion
            Android release version: $releaseVersion
            Android SDK version: $sdkVersion
            Android build ID: $buildID
            Device brand: $brand
            Device manufacturer: $manufacturer
            Device name: $device
            Device model: $model
            Device product name: $product
            Device hardware name: $hardware
            ABIs: ${Arrays.toString(abis)}
            ABIs (32bit): ${Arrays.toString(abis32Bits)}
            ABIs (64bit): ${Arrays.toString(abis64Bits)}
        """.trimIndent()
    }

    init {
        val packageInfo: PackageInfo? = try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        if (packageInfo != null) {
            versionCode = packageInfo.versionCode
            versionName = packageInfo.versionName
        } else {
            versionCode = -1
            versionName = null
        }
    }
}
