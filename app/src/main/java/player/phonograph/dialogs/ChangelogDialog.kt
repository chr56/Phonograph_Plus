package player.phonograph.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import lib.phonograph.localization.Localization
import mt.pref.ThemeColor
import mt.util.color.resolveColor
import org.intellij.lang.annotations.Language
import player.phonograph.R
import player.phonograph.notification.ErrorNotification
import player.phonograph.util.theme.nightMode
import androidx.fragment.app.DialogFragment
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.InflateException
import android.view.View
import android.webkit.WebView
import java.io.IOException
import java.io.InputStream
import java.util.*

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
            .positiveButton(android.R.string.ok) { it.dismiss() }
            .apply {
                getActionButton(WhichButton.POSITIVE).updateTextColor(
                    ThemeColor.accentColor(requireActivity())
                )
            }
        val webView = customView.findViewById<WebView>(R.id.web_view)

        // Fetch correct changelog

        try {
            val locale = Localization.currentLocale(requireContext())

            val inputStream = openLocalizedChangelogName(requireContext(), locale)

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
                    requireContext(),
                    R.attr.md_background_color,
                    Color.parseColor(if (requireContext().nightMode) "#424242" else "#ffffff")
                )
            )
        val textColor =
            colorToCSS(
                Color.parseColor(if (requireContext().nightMode) "#ffffff" else "#000000")
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

    fun openLocalizedChangelogName(context: Context, locale: Locale): InputStream {
        val fullSuffix = "${locale.language}-${locale.country}".uppercase()
        val mainSuffix = locale.language.uppercase()
        val assetManager = context.assets
        return try {
            assetManager.open("$changelog-$fullSuffix.html")
        } catch (e: IOException) {
            try {
                assetManager.open("$changelog-$mainSuffix.html")
            } catch (e: IOException) {
                assetManager.open("$changelog.html")
            }
        }
    }

    companion object {
        private const val changelog = "changelog"

        fun create(): ChangelogDialog = ChangelogDialog()

        private fun colorToCSS(color: Int): String =
            String.format(
                "rgb(%d, %d, %d)",
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            ) // on API 29, WebView doesn't load with hex colors

        @Language("CSS")
        fun getCSS(
            content_background_color: String,
            text_color: String,
            highlight_color: String,
            disable_color: String,
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
        h3 {
            margin-top: 1ex;
            margin-bottom: 1ex;
        }
        h4,
        h5 {
            padding: 0;
            margin: 0;
            margin-top: 2ex;
            margin-bottom: 0.5ex;
        }
        ol,
        ul {
            list-style-position: inside;
            border: 0;
            padding: 0;
            margin: 0;
            margin-left: 0.5ex;
        }
        li {
            padding: 1px;
            margin: 0;
            margin-left: 1ex;
        }
        p {
            margin: 0.75ex;
        }
        .highlight-text{
            color: $highlight_color;
        }
        .fine-print{
            color: $disable_color;
            font-size: small;
        }"""

        @Language("HTML")
        fun getHTML(
            CSS: String,
            content: String,
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
