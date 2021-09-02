package com.kabouzeid.gramophone.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import androidx.fragment.app.DialogFragment
import chr_56.MDthemer.core.ThemeColor
import chr_56.MDthemer.util.ColorUtil
import chr_56.MDthemer.util.Util
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import com.kabouzeid.gramophone.App
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.util.PreferenceUtil
import java.io.BufferedReader
import java.io.InputStreamReader
// Todo Review
/**
 * @author Aidan Follestad (afollestad)
 */
class ChangelogDialog : DialogFragment() {
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val customView: View = try {
            LayoutInflater.from(activity).inflate(R.layout.dialog_web_view, null)
        } catch (e: InflateException) {
            e.printStackTrace()
            return MaterialDialog(requireActivity())
                .title(android.R.string.dialog_alert_title)
                .message(text = "This device doesn't support web view, which is necessary to view the change log. It is missing a system component.")
                .positiveButton(android.R.string.ok)
        }
        val dialog: MaterialDialog = MaterialDialog(requireActivity())
            .title(R.string.changelog)
            .customView(view = customView, noVerticalPadding = false)
            .positiveButton(android.R.string.ok) { if (activity != null) setChangelogRead(requireActivity()) }
//                .showListener { dialog1 -> if (activity != null) setChangelogRead(requireActivity()) }
        //set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))

        val webView = customView.findViewById<WebView>(R.id.web_view)
        try {
            // Load from phonograph-changelog.html in the assets folder
            val buf = StringBuilder()
            val json = requireActivity().assets.open("phonograph-changelog.html")
            val `in` = BufferedReader(InputStreamReader(json, "UTF-8"))
            var str: String?
            while (`in`.readLine().also { str = it } != null) buf.append(str)
            `in`.close()
            val app = App.getInstance()

            // Inject color values for WebView body background and links
            val backgroundColor = colorToCSS(Util.resolveColor(activity, R.attr.md_background_color, Color.parseColor(if (app.nightmode()) "#424242" else "#ffffff")))
            val contentColor = colorToCSS(Color.parseColor(if (app.nightmode()) "#ffffff" else "#000000"))
            val changeLog = buf.toString()
                .replace("{style-placeholder}", String.format("body { background-color: %s; color: %s; }", backgroundColor, contentColor))
                .replace("{link-color}", colorToCSS(ThemeColor.accentColor(app))) // TODO MD
                .replace("{link-color-active}", colorToCSS(ColorUtil.lightenColor(ThemeColor.accentColor(app)))) // TODO MD
            webView.loadData(changeLog, "text/html", "UTF-8")
        } catch (e: Throwable) {
            webView.loadData("<h1>Unable to load</h1><p>" + e.localizedMessage + "</p>", "text/html", "UTF-8")
        }
        return dialog
    }

    companion object {
        @JvmStatic
        fun create(): ChangelogDialog {
            return ChangelogDialog()
        }

        @JvmStatic
        fun setChangelogRead(context: Context) {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = pInfo.versionCode
                PreferenceUtil.getInstance(context).setLastChangeLogVersion(currentVersion)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        private fun colorToCSS(color: Int): String {
            return String.format("rgb(%d, %d, %d)", Color.red(color), Color.green(color), Color.blue(color)) // on API 29, WebView doesn't load with hex colors
        }
    }
}
