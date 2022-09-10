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
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import player.phonograph.R
import player.phonograph.misc.VersionJson.Companion.DOWNLOAD_SOURCES
import player.phonograph.misc.VersionJson.Companion.DOWNLOAD_URIS
import player.phonograph.misc.VersionJson.Companion.EN
import player.phonograph.misc.VersionJson.Companion.LOG_SUMMARY
import player.phonograph.misc.VersionJson.Companion.VERSION
import player.phonograph.misc.VersionJson.Companion.VERSIONCODE
import player.phonograph.misc.VersionJson.Companion.ZH_CN
import player.phonograph.misc.VersionJson.Companion.separator
import player.phonograph.settings.Setting
import player.phonograph.util.UpdateUtil.CAN_ACCESS_GITHUB
import java.util.*

class UpgradeDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val activity = requireActivity()

        val versionInfo = requireArguments().getBundle(VERSION_BUNDLE) ?: Bundle()

        val versionCode = versionInfo.getInt(VERSIONCODE, -1)
        val version = versionInfo.getString(VERSION, "")
        val logZH = versionInfo.getString("${ZH_CN}${separator}${LOG_SUMMARY}", "")
        val logEN = versionInfo.getString("${EN}${separator}${LOG_SUMMARY}", "")
        val canAccessGitHub = versionInfo.getBoolean(CAN_ACCESS_GITHUB, false)

        val downloadUris: Array<String>? = versionInfo.getStringArray(DOWNLOAD_URIS)
        val downloadSources: Array<String>? = versionInfo.getStringArray(DOWNLOAD_SOURCES)

        // use correct log

        val log = when (requireContext().resources.configuration.locales.get(0).language) {
            Locale("zh").language,
            Locale("zh-rCN").language,
            Locale("zh-cn").language,
            Locale("zh-hans").language,
            -> {
                logZH
            }
            else -> {
                logEN
            }
        }

        val message = "<p>" +
            "<b>${getString(R.string.new_version_code)}</b>: $version <br/>" +
            "<b>${getString(R.string.new_version_log)}</b>: ${log?.replace("\n","<br/>") ?: "UNKNOWN"} <br/>" +
            "</p> " + "<br/>" +
            "<p style=\"color: grey;font: small;\"><br/>${getString(R.string.new_version_tips)}</p>"

        Log.d(this::class.simpleName, "Formatted Log:" + log?.replace("\\n", "<br/>"))

        val dialog = MaterialDialog(activity)
            .title(R.string.new_version)
            .message(text = Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT or Html.FROM_HTML_OPTION_USE_CSS_COLORS))
            .negativeButton(android.R.string.ok)
            .positiveButton(R.string.download) { _ ->

                val uris = mutableListOf<String>("https://github.com/chr56/Phonograph_Plus/releases")
                val text = mutableListOf<String>(getString(R.string.git_hub) + "(Release Page)")

                if (canAccessGitHub) {
                    uris.add("https://t.me/Phonograph_Plus")
                    text.add(getString(R.string.tg_channel))
                }
                if (downloadUris != null && downloadSources != null && (downloadUris.size == downloadSources.size)) {
                    uris.addAll(downloadUris)
                    text.addAll(downloadSources)
                }

                val downloadDialog = MaterialDialog(activity)
                    .title(R.string.download)
                    .listItemsSingleChoice(items = text, waitForPositiveButton = true) { downloadDialog: MaterialDialog, index: Int, _: CharSequence ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = Uri.parse(uris[index])
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        activity.startActivity(intent)
                        downloadDialog.dismiss() // dismiss DownloadDialog
                    }
                downloadDialog
                    .positiveButton(android.R.string.ok) { downloadDialog.dismiss() }
                    .negativeButton(android.R.string.cancel) { downloadDialog.dismiss() }
                    .show()
            }
            .neutralButton(R.string.ignore_once) { _ ->
                dismiss()
                Setting.instance.ignoreUpgradeVersionCode = versionCode
                Toast.makeText(activity, R.string.upgrade_ignored, Toast.LENGTH_SHORT).show()
            }
        return dialog
    }

    companion object {
        fun create(versionInfo: Bundle): UpgradeDialog {
//            val dialog = UpgradeDialog()
//            dialog.arguments = Bundle().also {
//                it.putBundle(VERSION_BUNDLE, versionInfo)
//            }
//            return dialog
            return UpgradeDialog().apply {
                this.arguments = Bundle().also {
                    it.putBundle(VERSION_BUNDLE, versionInfo)
                }
            }
        }

        private const val VERSION_BUNDLE = "VERSION_BUNDLE"
    }
}
