/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.App
import player.phonograph.R
import player.phonograph.migrate.DatabaseBackupManger
import player.phonograph.misc.ICreateFileStorageAccess
import player.phonograph.misc.IOpenFileStorageAccess
import player.phonograph.misc.OpenDocumentContract
import player.phonograph.util.CoroutineUtil
import player.phonograph.util.TimeUtil
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import android.app.Activity
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupDataDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        require(activity is IOpenFileStorageAccess && activity is ICreateFileStorageAccess) {
            "Unsupported activity (SAF unavailable)"
        }
        val menu = listOf(
            "export path filter" to {
                export(
                    requireActivity(),
                    "phonograph_plus_path_filter_${TimeUtil.currentDateTime()}.json"
                ) { uri ->
                    DatabaseBackupManger.exportPathFilter(activity, uri)
                    true
                }
            },
            "import path filter" to {
                import(requireActivity(), "application/json") { uri ->
                    DatabaseBackupManger.importPathFilter(activity, uri)
                    true
                }
            },
            "export playing queues" to {
                export(
                    requireActivity(),
                    "phonograph_plus_playing_queues_${TimeUtil.currentDateTime()}.json"
                ) { uri ->
                    DatabaseBackupManger.exportPlayingQueues(activity, uri)
                    true
                }
            },
            "import playing queues" to {
                import(requireActivity(), "application/json") { uri ->
                    DatabaseBackupManger.importPlayingQueues(activity, uri)
                    true
                }
            },
        )
        return AlertDialog.Builder(activity)
            .setTitle("Backup")
            .setItems(menu.map { it.first }.toTypedArray()) { dialog, i ->
                dialog.dismiss()
                menu[i].second.invoke()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .create()
    }

    private fun import(activity: Activity, mimeType: String, callback: (Uri) -> Boolean) {
        require(activity is IOpenFileStorageAccess && activity is ICreateFileStorageAccess) {
            "Unsupported activity (SAF unavailable)"
        }
        activity.openFileStorageAccessTool.launch(
            OpenDocumentContract.Cfg(null, arrayOf(mimeType))
        ) { uri: Uri? ->
            uri ?: return@launch
            lifecycleScope.launch(Dispatchers.IO) {
                callback(uri).andReport()
            }
        }
    }

    private fun export(activity: Activity, fileName: String, callback: (Uri) -> Boolean) {
        require(activity is IOpenFileStorageAccess && activity is ICreateFileStorageAccess) {
            "Unsupported activity (SAF unavailable)"
        }
        activity.createFileStorageAccessTool.launch(fileName) { uri: Uri? ->
            uri ?: return@launch
            lifecycleScope.launch(Dispatchers.IO) {
                callback(uri).andReport()
            }
        }
    }

    private suspend fun Boolean.andReport() {
        CoroutineUtil.coroutineToast(App.instance, if (this) R.string.success else R.string.failed)
    }
}