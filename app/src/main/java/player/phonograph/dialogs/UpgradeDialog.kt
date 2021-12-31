/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.dialogs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import player.phonograph.App
import player.phonograph.R
import player.phonograph.Updater
import player.phonograph.util.PreferenceUtil

class UpgradeDialog : DialogFragment() {
    private var versionCode: Int = -1
    private var version: String? = null
    private var log: String? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val arguments = requireArguments()
        val activity = requireActivity()

        versionCode = arguments.getInt(Updater.VersionCode)
        version = arguments.getString(Updater.Version)
        log = arguments.getString(Updater.LogSummary)

        val message = "<p>" +
            "<b>${getString(R.string.new_version_code)}</b>: $version <br/>" +
            "<b>${getString(R.string.new_version_log)}</b>: ${log?.replace("\n","<br/>") ?: "UNKNOWN"} <br/>" +
            "</p> " + "<br/>" +
            "<p style=\"color: grey;font: small;\"><br/>${getString(R.string.new_version_tips)}</p>"

        Log.w(this::class.simpleName, "Formatted Log:" + log?.replace("\\n", "<br/>"))

        val dialog = MaterialDialog(activity)
            .title(R.string.new_version)
            .message(text = Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT or Html.FROM_HTML_OPTION_USE_CSS_COLORS))
            .negativeButton(android.R.string.ok)
            .positiveButton(R.string.download) { _ ->
                dismiss() // dismiss UpgradeDialog
                val uris = mutableListOf<String>("https://github.com/chr56/Phonograph_Plus/releases", "https://t.me/Phonograph_Plus")
                val text = mutableListOf<String>(getString(R.string.git_hub), getString(R.string.tg_channel))
                val downloadDialog = MaterialDialog(activity)
                    .title(R.string.download)
                    .listItemsSingleChoice(items = text, waitForPositiveButton = true) { d: MaterialDialog, index: Int, _: CharSequence ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(uris[index])
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        activity.startActivity(intent)
                        d.dismiss() // dismiss DownloadDialog
                    }
                downloadDialog
                    .positiveButton(android.R.string.ok) { downloadDialog.dismiss() }
                    .negativeButton(android.R.string.cancel) { downloadDialog.dismiss() }
                    .show()
            }
            .neutralButton(R.string.ignore_once) { _ ->
                dismiss()
                PreferenceUtil(App.instance).ignoreUpgradeVersionCode = versionCode
            }
//            .positiveButton(R.string.git_hub, null) {
//                val i = Intent(Intent.ACTION_VIEW)
//                i.data = Uri.parse("https://github.com/chr56/Phonograph_Plus/releases")
//                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                startActivity(i)
//            }
//            .negativeButton(R.string.tg_channel, null) {
//                val i = Intent(Intent.ACTION_VIEW)
//                i.data = Uri.parse("https://t.me/Phonograph_Plus")
//                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                startActivity(i)
//            }
        return dialog
    }

    companion object {
        fun create(versionInfo: Bundle): UpgradeDialog {
            val dialog = UpgradeDialog()
            val args = Bundle()
            args.putInt(Updater.VersionCode, versionInfo.getInt(Updater.VersionCode))
            args.putString(Updater.Version, versionInfo.getString(Updater.Version))
            args.putString(Updater.LogSummary, versionInfo.getString(Updater.LogSummary))
            dialog.arguments = args
            return dialog
        }
    }
}
