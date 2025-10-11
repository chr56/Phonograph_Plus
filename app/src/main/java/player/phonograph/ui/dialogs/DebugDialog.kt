/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.App
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.Update
import player.phonograph.model.Song
import player.phonograph.model.version.VersionCatalog
import player.phonograph.repo.mediastore.MediaStoreSongsActions
import player.phonograph.ui.modules.main.MainActivity
import player.phonograph.util.concurrent.coroutineToast
import player.phonograph.util.theme.tintButtons
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class DebugDialog : DialogFragment() {

    @OptIn(DelicateCoroutinesApi::class)
    private val items = listOf<Pair<String, Function1<DialogInterface, Unit>>>(
        "Crash the app" to {
            throw Exception("Crash Test!!! Crash Test!!! Crash Test!!! Crash Test!!! Crash Test!!! ")
        },
        "Crash the app (Coroutine)" to {
            GlobalScope.launch {
                throw Exception("Crash Test!!! Crash Test!!! Crash Test!!! Crash Test!!! Crash Test!!! ")
            }
        },
        "Send Crash Notification" to {
            warning(
                App.instance,
                "Debug",
                "Crash Notification Test!",
                Exception("Test"),
            )
        },
        "Check Overflowed Song Ids" to {
            CoroutineScope(Dispatchers.IO).launch {
                val errors = MediaStoreSongsActions.checkEmbeddedIdOverflow(App.instance)
                dumpSong("Overflowed Ids", errors)
            }
        },
        "Check Conflicted Song Ids" to {
            CoroutineScope(Dispatchers.IO).launch {
                val errors = MediaStoreSongsActions.checkIdConflict(App.instance)
                dumpSong("Conflicted Position Embedded Ids", errors)
            }
        },
        "Check for updates (Dialog)" to {
            CoroutineScope(Dispatchers.Unconfined).launch {
                Update.checkUpdate(true) { versionCatalog: VersionCatalog, upgradable: Boolean ->
                    try {
                        UpgradeInfoDialog.create(versionCatalog)
                            .show(hostActivity.get()?.supportFragmentManager!!, "DebugDialog")
                        if (!upgradable) {
                            coroutineToast(App.instance, "not upgradable")
                        }
                    } catch (e: IllegalStateException) {
                        Log.e("CheckUpdateCallback", e.message.orEmpty())
                    }
                }
            }
        },
        "Check for updates (Notification)" to {
            CoroutineScope(Dispatchers.Unconfined).launch {
                Update.checkUpdate(true) { versionCatalog: VersionCatalog, upgradable: Boolean ->
                    Update.sendNotification(
                        App.instance,
                        versionCatalog,
                        MainActivity.launchingIntent(
                            App.instance,
                            versionCatalog,
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        )
                    )
                    if (!upgradable) {
                        coroutineToast(App.instance, "not upgradable")
                    }
                }
            }
        },
    )

    private suspend fun dumpSong(title: String, errors: Collection<Song>) {
        val message = errors.fold("$title\n:") { acc, song -> "$acc\n${song.id}: ${song.title}" }
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(hostActivity.get()!!)
                .setTitle(title)
                .setMessage(message)
                .show()
        }
    }


    private lateinit var hostActivity: WeakReference<FragmentActivity>
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val (texts, callbacks) = items.unzip()
        return AlertDialog.Builder(requireContext())
            .setTitle("Debug Menu")
            .setSingleChoiceItems(texts.toTypedArray(), -1) { dialog, index ->
                dialog.dismiss()
                callbacks[index].invoke(dialog)
            }
            .create().tintButtons()
    }

    override fun onStart() {
        hostActivity = WeakReference(requireActivity())
        super.onStart()
    }
}
