package com.kabouzeid.gramophone.ui.activities

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.kabouzeid.gramophone.App
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.ui.activities.base.ThemeActivity
import com.kabouzeid.gramophone.ui.activities.bugreport.model.DeviceInfo
import java.util.*

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
        val devicedata: String = collectData()

        // other data
        val otherInfo: String = DeviceInfo(this).toString()

        // appended string
        val displayText: String =
            "Crash Report:\n\n$devicedata\n$stackTraceText\nOther detailed info:\n$otherInfo"

        // display textview
        val textView = findViewById<TextView>(R.id.crash_text)
        @SuppressLint("SetTextI18n")
        textView.text = displayText

        // button
        val button = findViewById<Button>(R.id.copy_to_clipboard)
        if (stackTraceText.isEmpty()) {
            button.visibility = View.GONE
        } else {
            button.visibility = View.VISIBLE
            // copy to clipboard
            button.setOnClickListener {
                val clipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("CRASH", displayText)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show()
            }
        }
    }

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
