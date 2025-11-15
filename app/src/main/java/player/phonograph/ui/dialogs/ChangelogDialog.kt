package player.phonograph.ui.dialogs

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import player.phonograph.R
import player.phonograph.foundation.error.warning
import player.phonograph.foundation.localization.LocalizationStore
import player.phonograph.settings.PrerequisiteSetting
import player.phonograph.util.currentVersionCode
import player.phonograph.util.text.changelogCSS
import player.phonograph.util.text.changelogHTML
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.themeCardBackgroundColor
import player.phonograph.util.theme.tintButtons
import androidx.appcompat.app.AlertDialog
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
import java.util.Locale

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
                val msg =
                    "This device doesn't support web view, which is necessary to view the change log. It is missing a system component."
                warning(requireContext(), "ChangelogDialog", msg, e)
                return AlertDialog.Builder(requireActivity())
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .tintButtons()
            }

        val dialog = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.label_changelog)
            .setView(customView)
            .setPositiveButton(android.R.string.ok) { dialog, which ->
                val context = requireContext()
                PrerequisiteSetting.instance(context).lastChangelogVersion = currentVersionCode(context)
            }
            .create()
            .tintButtons()

        val webView = customView.findViewById<WebView>(R.id.web_view)

        // Fetch correct changelog

        try {
            val locale = LocalizationStore.current(requireContext())

            val inputStream = openLocalizedChangelogName(requireContext(), locale)

            // Process changelog.html in the assets folder
            val content = inputStream.use { stream ->
                stream.reader(Charsets.UTF_8).use {
                    it.readText()
                }
            }

            val changeLog = generateChangelogHTML(content, accentColor())

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
            themeCardBackgroundColor(requireContext())

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
