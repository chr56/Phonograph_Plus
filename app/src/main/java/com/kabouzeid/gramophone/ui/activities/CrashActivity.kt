package com.kabouzeid.gramophone.ui.activities

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.kabouzeid.gramophone.App
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.ui.activities.base.ThemeActivity

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

        // other data
        val otherInfo: String = collectData()

        // display textview
        val textView = findViewById<TextView>(R.id.crash_text)
        @SuppressLint("SetTextI18n")
        textView.text =
            "Crash Report:\n\n$otherInfo\n$stackTraceText"

        // button
        val button = findViewById<Button>(R.id.copy_to_clipboard)
        if (stackTraceText.isEmpty()) {
            button.visibility = View.GONE
        } else {
            button.visibility = View.VISIBLE
            // clipboard
            button.setOnClickListener {
                val clipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("CRASH",             "Crash Report:\n$otherInfo\n$stackTraceText")
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

        packageInfo?.let {
            versionName = packageInfo.versionName
            packageName = packageInfo.packageName
        }

        val buffer: StringBuffer = StringBuffer()
        buffer.append("packageName $packageName").appendLine()
            .append("versionName $versionName").appendLine()
        return buffer.toString()
    }
}
