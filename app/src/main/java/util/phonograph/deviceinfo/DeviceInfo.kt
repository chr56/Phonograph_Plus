package util.phonograph.deviceinfo

import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.util.currentVersionCode
import player.phonograph.util.currentVersionName
import androidx.annotation.IntRange
import android.os.Build
import java.util.Arrays

class DeviceInfo {

    private var versionCode = -1
    private var versionName: String? = null
    private val buildVersion = Build.VERSION.INCREMENTAL
    private val releaseVersion = Build.VERSION.RELEASE
    private val packageName = BuildConfig.APPLICATION_ID

    init {
        versionName = currentVersionName(App.instance)
        versionCode = currentVersionCode(App.instance)
    }

    @IntRange(from = 0)
    private val sdkVersion = Build.VERSION.SDK_INT
    private val buildID = Build.DISPLAY
    private val brand = Build.BRAND
    private val manufacturer = Build.MANUFACTURER
    private val device = Build.DEVICE
    private val model = Build.MODEL
    private val product = Build.PRODUCT
    private val hardware = Build.HARDWARE

    @Suppress("PrivatePropertyName")
    private val ABIS = Build.SUPPORTED_ABIS

    @Suppress("PrivatePropertyName")
    private val ABIS32 = Build.SUPPORTED_32_BIT_ABIS

    @Suppress("PrivatePropertyName")
    private val ABIS64 = Build.SUPPORTED_64_BIT_ABIS

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
               <tr><td>ABIs</td><td>${Arrays.toString(ABIS)}</td></tr>
               <tr><td>ABIs (32bit)</td><td>${Arrays.toString(ABIS32)}</td></tr>
               <tr><td>ABIs (64bit)</td><td>${Arrays.toString(ABIS64)}</td></tr>
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
            ABIs: ${Arrays.toString(ABIS)}
            ABIs (32bit): ${Arrays.toString(ABIS32)}
            ABIs (64bit): ${Arrays.toString(ABIS64)}
        """.trimIndent()
    }
}
