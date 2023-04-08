/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.model.version.VersionCatalog
import player.phonograph.notification.ErrorNotification
import player.phonograph.notification.UpgradeNotification
import player.phonograph.util.coroutineToast
import player.phonograph.mechanism.Update
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

class DebugDialog : DialogFragment() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val debugMenuItem = listOf(
            "Crash the app",
            "Crash the app (Coroutine)",
            "Send Crash Notification",
            "Check Upgrade (Dialog)",
            "Check Upgrade (Notification)",
        )
        val fragmentManager = requireActivity().supportFragmentManager
        val dialog = MaterialDialog(requireActivity())
            .title(text = "Debug Menu")
            .listItemsSingleChoice(items = debugMenuItem) { dialog: MaterialDialog, index: Int, _: CharSequence ->
                when (index) {
                    0 -> throw Exception("Crash Test!!! Crash Test!!! Crash Test!!! Crash Test!!! Crash Test!!! ")
                    1 -> GlobalScope.launch {
                        throw Exception("Crash Test!!! Crash Test!!! Crash Test!!! Crash Test!!! Crash Test!!! ")
                    }
                    2 -> ErrorNotification.postErrorNotification(Exception("Test"), "Crash Notification Test!!")
                    3 -> {
                        CoroutineScope(Dispatchers.Unconfined).launch {
                            Update.checkUpdate(true) { versionCatalog: VersionCatalog, upgradable: Boolean ->
                                try {
                                    player.phonograph.ui.dialogs.UpgradeDialog.create(versionCatalog).show(fragmentManager, "DebugDialog")
                                    if (!upgradable) {
                                        coroutineToast(App.instance, "not upgradable")
                                    }
                                } catch (e: IllegalStateException) {
                                    Log.e("CheckUpdateCallback", e.message.orEmpty())
                                }
                            }
                        }
                    }
                    4 -> {
                        CoroutineScope(Dispatchers.Unconfined).launch {
                            Update.checkUpdate(true) { versionCatalog: VersionCatalog, upgradable: Boolean ->
                                val channel = when (BuildConfig.FLAVOR) {
                                    "preview" -> "preview"
                                    else -> "stable"
                                }
                                UpgradeNotification.sendUpgradeNotification(versionCatalog, channel)
                                if (!upgradable) {
                                    coroutineToast(App.instance, "not upgradable")
                                }
                            }
                        }
                    }
                    else -> dialog.dismiss()
                }
            }
        return dialog
    }
}
