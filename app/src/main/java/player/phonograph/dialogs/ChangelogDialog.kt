package player.phonograph.dialogs

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import lib.phonograph.localization.Localization
import mt.pref.ThemeColor
import mt.util.color.resolveColor
import player.phonograph.R
import player.phonograph.notification.ErrorNotification
import player.phonograph.util.text.changelogCSS
import player.phonograph.util.text.changelogHTML
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

    private fun generateChangelogHTML(content: String, accentColor: Int): String {
        val backgroundColor =
            resolveColor(
                requireContext(),
                R.attr.md_background_color,
                Color.parseColor(if (requireContext().nightMode) "#424242" else "#ffffff")
            )

        val textColor =
            Color.parseColor(if (requireContext().nightMode) "#ffffff" else "#000000")

        return changelogHTML(
            CSS = changelogCSS(
                content_background_color = backgroundColor,
                text_color = textColor,
                highlight_color = accentColor,
                disable_color = Color.rgb(167, 167, 167)
            ),
            content = content
        )
    }

    private fun openLocalizedChangelogName(context: Context, locale: Locale): InputStream {
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
    }
}
