package player.phonograph.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.InflateException
import android.view.View
import android.webkit.WebView
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import player.phonograph.App
import player.phonograph.R
import player.phonograph.settings.Setting
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.Util
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

/**
 * @author Aidan Follestad (afollestad)
 */
class ChangelogDialog : DialogFragment() {
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val customView: View = try {
            requireActivity().layoutInflater.inflate(R.layout.dialog_web_view, null)
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
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))

        val webView = customView.findViewById<WebView>(R.id.web_view)
        try {
            // Fetch correct changelog
            val locale = requireContext().resources.configuration.locales.get(0)

            val inputStream: InputStream = when (locale.language) {
                Locale("zh").language,
                Locale("zh-rCN").language,
                Locale("zh-cn").language,
                Locale("zh-hans").language,
                -> {
                    requireActivity().assets.open("phonograph-changelog-zh-rCN.html")
                }
                else -> {
                    requireActivity().assets.open("phonograph-changelog.html")
                }
            }

            // Process changelog.html in the assets folder
            val output = StringBuilder()
            val bufferedReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

            var buffer: String?
            while (bufferedReader.readLine().also { buffer = it } != null) {
                output.append(buffer)
            }
            bufferedReader.close()

            // Inject color values for WebView body background and links
            val backgroundColor = colorToCSS(Util.resolveColor(activity, R.attr.md_background_color, Color.parseColor(if (App.instance.nightmode()) "#424242" else "#ffffff")))
            val contentColor = colorToCSS(Color.parseColor(if (App.instance.nightmode()) "#ffffff" else "#000000"))
            val changeLog = output.toString()
                .replace("CONTENT-BACKGROUND-COLOR", backgroundColor)
                .replace("TEXT-COLOR", contentColor)
                .replace("DISABLE-COLOR", "rgb(167, 167, 167)") // grey
                .replace("HIGHLIGHT-COLOR", colorToCSS(ThemeColor.accentColor(App.instance)))

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
                Setting.instance.lastChangeLogVersion = currentVersion
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        private fun colorToCSS(color: Int): String {
            return String.format("rgb(%d, %d, %d)", Color.red(color), Color.green(color), Color.blue(color)) // on API 29, WebView doesn't load with hex colors
        }
    }
}
