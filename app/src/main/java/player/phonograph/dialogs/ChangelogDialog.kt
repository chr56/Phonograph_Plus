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
import java.io.InputStream
import java.util.*
import lib.phonograph.localization.Localization
import player.phonograph.App
import player.phonograph.R
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.Util.resolveColor

/**
 * @author Aidan Follestad (afollestad)
 */
class ChangelogDialog : DialogFragment() {
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val customView: View =
            try {
                requireActivity().layoutInflater.inflate(R.layout.dialog_web_view, null)
            } catch (e: InflateException) {
                ErrorNotification.postErrorNotification(e)
                return MaterialDialog(requireActivity())
                    .title(android.R.string.dialog_alert_title)
                    .message(
                        text = "This device doesn't support web view, which is necessary to view the change log. It is missing a system component."
                    )
                    .positiveButton(android.R.string.ok)
            }

        val dialog: MaterialDialog = MaterialDialog(requireActivity())
            .title(R.string.changelog)
            .customView(view = customView, noVerticalPadding = false)
            .positiveButton(android.R.string.ok) { setChangelogRead(requireActivity()) }
            .apply {
                getActionButton(WhichButton.POSITIVE).updateTextColor(
                    ThemeColor.accentColor(requireActivity())
                )
            }
        val webView = customView.findViewById<WebView>(R.id.web_view)

        // Fetch correct changelog
        val locale = Localization.currentLocale(requireContext())

        val changelogFileName =
            when (locale.language) {
                Locale.SIMPLIFIED_CHINESE.language -> {
                    "changelog-zh-CN.html"
                }
                else -> {
                    "changelog.html"
                }
            }

        try {
            val inputStream: InputStream = requireActivity().assets.open(changelogFileName)

            // Process changelog.html in the assets folder
            val content = inputStream.use { stream ->
                stream.reader(Charsets.UTF_8).use {
                    it.readText()
                }
            }

            val changeLog = generateChangelogHTML(
                content,
                ThemeColor.accentColor(requireContext())
            )
            // if (DEBUG) Log.v("CHANGELOG", changeLog)

            webView.loadData(changeLog, "text/html", "UTF-8")
        } catch (e: Throwable) {
            webView.loadData(
                """<h1>Unable to load</h1><p>${e.localizedMessage}</p>""",
                "text/html",
                "UTF-8"
            )
        }

        return dialog
    }

    fun generateChangelogHTML(content: String, accentColor: Int): String {
        val backgroundColor =
            colorToCSS(
                resolveColor(
                    activity,
                    R.attr.md_background_color,
                    Color.parseColor(if (App.instance.nightMode) "#424242" else "#ffffff")
                )
            )
        val textColor =
            colorToCSS(
                Color.parseColor(if (App.instance.nightMode) "#ffffff" else "#000000")
            )
        return getHTML(
            CSS = getCSS(
                content_background_color = backgroundColor,
                text_color = textColor,
                highlight_color = colorToCSS(accentColor),
                disable_color = "rgb(167, 167, 167)"
            ),
            content = content
        )
    }

    companion object {

        fun create(): ChangelogDialog = ChangelogDialog()

        fun setChangelogRead(context: Context) {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = pInfo.versionCode
                Setting.instance.lastChangeLogVersion = currentVersion
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        private fun colorToCSS(color: Int): String =
            String.format(
                "rgb(%d, %d, %d)",
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            ) // on API 29, WebView doesn't load with hex colors

        fun getCSS(
            content_background_color: String,
            text_color: String,
            highlight_color: String,
            disable_color: String
        ) = """
        * {
            word-wrap: break-word;
        }
        body {
            background-color: $content_background_color;
            color: $text_color;
        }
        a {
            color: $highlight_color;
        }
        a:active {
            color: $highlight_color;
        }
        ol {
            list-style-position: inside;
            padding-left: 0;
            padding-right: 0;
        }
        ul {
            list-style-position: inside;
            padding-left: 0;
            padding-right: 0;
        }
        li {
            padding-top: 2px;
        }
        .highlight-text{
            color: $highlight_color;
        }
        .fine-print{
            color: $disable_color;
            font-size: small;
        }"""

        fun getHTML(
            CSS: String,
            content: String
        ): String =
            """
        <html>
        <head>
        <style type="text/css">
        $CSS
        </style>
        </head>
        <body>
        $content
        </body>
        </html>
        """
    }
}
