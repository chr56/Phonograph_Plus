package com.kabouzeid.gramophone.ui.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
        val stackTrace: String? = intent.getStringExtra(App.KEY_STACK_TRACE)
        val textView = findViewById<TextView>(R.id.crash_text)
        textView.text = getString(R.string.Crash)
        stackTrace?.let {
            textView.text = it
        }

        // button
        val button = findViewById<Button>(R.id.copy_to_clipboard)
        if (stackTrace.isNullOrEmpty()) {
            button.visibility = View.GONE
        } else {
            button.visibility = View.VISIBLE
            // clipboard
            button.setOnClickListener {
                val clipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("CRASH", stackTrace)
                clipboardManager.setPrimaryClip(clipData)
                Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
