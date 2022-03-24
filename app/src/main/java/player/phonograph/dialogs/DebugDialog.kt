/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.App
import player.phonograph.R
import player.phonograph.Updater
import player.phonograph.notification.UpgradeNotification
import player.phonograph.settings.Setting
import player.phonograph.util.Util.coroutineToast
import java.lang.ref.WeakReference

class DebugDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val attachedActivity: WeakReference<FragmentActivity> = WeakReference(requireActivity())

        val debugMenuItem = listOf(
            "Crash the app",
            "Check Upgrade (Dialog)",
            "Check Upgrade (Notification)",
        )

        val dialog = MaterialDialog(requireActivity())
            .title(text = "Debug Menu")
            .listItemsSingleChoice(items = debugMenuItem) { dialog: MaterialDialog, index: Int, _: CharSequence ->
                when (index) {
                    0 -> throw Exception("Crash Test!!! Crash Test!!! Crash Test!!! Crash Test!!! Crash Test!!! ")
                    1 -> {
                        Updater.checkUpdate(callback = {
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    UpgradeDialog.create(it).show(attachedActivity.get()!!.supportFragmentManager, "DebugDialog")
                                    if (Setting.instance.ignoreUpgradeVersionCode >= it.getInt(Updater.VERSIONCODE)) {
                                        coroutineToast(App.instance, getString(R.string.upgrade_ignored))
                                    }
                                } catch (e: IllegalStateException) {
                                    Log.e("CheckUpdateCallback", e.message.orEmpty())
                                }
                            }
                        }, force = true)
                    }
                    2 -> {
                        Updater.checkUpdate(callback = {
                            UpgradeNotification.sendUpgradeNotification(it)
                            CoroutineScope(Dispatchers.Main).launch {
                                if (Setting.instance.ignoreUpgradeVersionCode >= it.getInt(Updater.VERSIONCODE)) {
                                    coroutineToast(App.instance, getString(R.string.upgrade_ignored))
                                }
                            }
                        }, force = true)
                    }
                    else -> dialog.dismiss()
                }
            }
        return dialog
    }
}
