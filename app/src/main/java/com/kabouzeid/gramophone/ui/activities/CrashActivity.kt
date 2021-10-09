package com.kabouzeid.gramophone.ui.activities

import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IntRange
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.kabouzeid.gramophone.App
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.ui.activities.base.ThemeActivity
import com.kabouzeid.gramophone.util.PreferenceUtil
import java.util.*
import kotlin.system.exitProcess

class CrashActivity : ThemeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        // statusbar theme
        setStatusbarColor(resources.getColor(R.color.md_grey_800))
        setDrawUnderStatusbar()

        // toolbar theme
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(resources.getColor(R.color.md_grey_700))
        toolbar.title = getString(R.string.Crash)
        setSupportActionBar(toolbar)

        // stack trace text
        var stackTraceText: String = "No Stack Trace !?"
        intent.getStringExtra(App.KEY_STACK_TRACE)?.let { stackTraceText = it }

        // device data
        val deviceInfo: String = getDeviceInfo(this)

        // appended string
        val displayText: String =
            "Crash Report:\n\n$deviceInfo\n$stackTraceText\n"

        // display textview
        val textView = findViewById<TextView>(R.id.crash_text)
        @SuppressLint("SetTextI18n")
        textView.text = displayText

        // button "copy to clipboard"
        val buttonCopy = findViewById<Button>(R.id.copy_to_clipboard)
        if (stackTraceText.isEmpty()) {
            buttonCopy.visibility = View.GONE
        } else {
            buttonCopy.visibility = View.VISIBLE
            // copy to clipboard
            buttonCopy.setOnClickListener {
                val clipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("CRASH", displayText)
                clipboardManager.setPrimaryClip(clipData)
            }
        }

        // button "clear all preference"
        val buttonReset = findViewById<Button>(R.id.clear_all_preference)
        buttonReset.setOnClickListener {
            val dialog: MaterialDialog = MaterialDialog(this)
                .title(R.string.clear_all_preference)
                .message(R.string.clear_all_preference_msg)
                .negativeButton(android.R.string.cancel)
                .positiveButton(R.string.clear_all_preference) {
                    PreferenceUtil.getInstance(applicationContext).clearAllPreference(applicationContext)

                    Handler().postDelayed({
                        Process.killProcess(Process.myPid())
                        exitProcess(1);
                    },4000)

                }
                .cancelOnTouchOutside(true)

            dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(getColor(R.color.md_red_A700))

            dialog.show()

        }
    }

    @Deprecated("abandoned")
    private fun collectData(): String {
        val pm: PackageManager = this.packageManager
        val packageInfo: PackageInfo? = pm.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)

        var packageName: String = "null"
        var versionName: String = "null"
        val osVersion: String = getOsString()
        val deviceInfo: String = getDeviceString()
        val appLanguage: String = Locale.getDefault().language

        packageInfo?.let {
            versionName = packageInfo.versionName
            packageName = packageInfo.packageName
        }

        val buffer: StringBuffer = StringBuffer()
        buffer.append("packageName $packageName").appendLine()
            .append("versionName $versionName").appendLine()
            .append("osVersion   $osVersion").appendLine()
            .append("deviceInfo  $deviceInfo").appendLine()
            .append("appLanguage $appLanguage").appendLine()

        return buffer.toString()
    }

    companion object {
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
            /*
            val abis: String =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) Build.SUPPORTED_ABIS.toString()
                else "${Build.CPU_ABI},${Build.CPU_ABI2}"
            val abis32Bits: String =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) Build.SUPPORTED_32_BIT_ABIS.toString()
                else "not available"
            val abis64Bits: String =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) Build.SUPPORTED_64_BIT_ABIS.toString()
                else "not available"
            */
//            ABIs: $abis
//            ABIs (32bit): $abis32Bits
//            ABIs (64bit): $abis64Bits

            return """
            Package name:    $packageName
            App version:     $versionName ($versionCode)
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

        @Deprecated("abandoned")
        fun getOsString(): String {
            val buffer: StringBuffer = StringBuffer()

            // 
            // OS Name&version
            //
            System.getProperty("os.name")?.let { buffer.append(it).append(" ") }

            val osName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                Build.VERSION.BASE_OS else "Android"
            if (!osName.isNullOrEmpty()) {
                buffer.append(osName).append(" ")
            }

            buffer.append("Android")
            buffer.append(Build.VERSION.RELEASE).append(" ")
            buffer.append(Build.VERSION.SDK_INT).append(" ")
            buffer.append(Build.VERSION.SECURITY_PATCH)

            return buffer.toString()
        }

        @Deprecated("abandoned")
        fun getDeviceString(): String {
            val buffer: StringBuffer = StringBuffer()

            //
            // Brand & Device
            //
            buffer.append(Build.BRAND).append(": ")
            buffer.append(Build.MODEL).append(" ")
            buffer.append(Build.DEVICE).append(" ")

            return buffer.toString()
        }
    }
}
