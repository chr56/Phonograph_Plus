/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.dialogs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import player.phonograph.R
import player.phonograph.Updater

class UpgradeDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val versionInfo = Updater.result!!

        val versionCode = versionInfo.getInt(Updater.VersionCode)
        val version = versionInfo.getString(Updater.Version)
        val log = versionInfo.getString(Updater.LogSummary)

        val dialog = MaterialDialog(requireActivity())
            .title(R.string.new_version)
            .message(text = getString(R.string.new_version_msg, version, log))
            .neutralButton(android.R.string.ok, null, null)
            .positiveButton(R.string.git_hub, null) {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse("https://github.com/chr56/Phonograph_Plus/releases")
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
            }
            .negativeButton(R.string.tg_channel, null){
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse("https://t.me/Phonograph_Plus")
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(i)
            }
        return dialog
    }
}
