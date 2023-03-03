/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.App
import player.phonograph.R
import player.phonograph.migrate.DatabaseBackupManger
import player.phonograph.migrate.SettingDataManager
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenDocumentContract
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

        val stringImport = resources.getString(R.string.import_)
        val stringExport = resources.getString(R.string.export_)
        val stringSetting = resources.getString(R.string.action_settings)
        val stringPathFilter = resources.getString(R.string.path_filter)
        val stringPlayingQueue = resources.getString(R.string.label_playing_queue)
        val stringFavorite = resources.getString(R.string.favorites)

        val menu = listOf(
            "$stringExport$stringSetting" to {
                export(
                    requireActivity(),
                    "phonograph_plus_favorites_${TimeUtil.currentDateTime()}.json"
                ) { uri ->
                    SettingDataManager.exportSettings(uri, activity)
                    true
                }
            },
            "$stringImport$stringSetting" to {
                import(requireActivity(), "application/json") { uri ->
                    SettingDataManager.importSetting(uri, activity)
                    true
                }
            },
            "$stringExport$stringPathFilter" to {
                export(
                    requireActivity(),
                    "phonograph_plus_path_filter_${TimeUtil.currentDateTime()}.json"
                ) { uri ->
                    DatabaseBackupManger.exportPathFilter(activity, uri)
                    true
                }
            },
            "$stringImport$stringPathFilter" to {
                import(requireActivity(), "application/json") { uri ->
                    DatabaseBackupManger.importPathFilter(activity, uri)
                    true
                }
            },
            "$stringExport$stringPlayingQueue" to {
                export(
                    requireActivity(),
                    "phonograph_plus_playing_queues_${TimeUtil.currentDateTime()}.json"
                ) { uri ->
                    DatabaseBackupManger.exportPlayingQueues(activity, uri)
                    true
                }
            },
            "$stringImport$stringPlayingQueue" to {
                import(requireActivity(), "application/json") { uri ->
                    DatabaseBackupManger.importPlayingQueues(activity, uri)
                    true
                }
            },
            "$stringExport$stringFavorite" to {
                export(
                    requireActivity(),
                    "phonograph_plus_favorites_${TimeUtil.currentDateTime()}.json"
                ) { uri ->
                    DatabaseBackupManger.exportFavorites(activity, uri)
                    true
                }
            },
            "$stringImport$stringFavorite" to {
                import(requireActivity(), "application/json") { uri ->
                    DatabaseBackupManger.importFavorites(activity, uri)
                    true
                }
            },
        )
        return AlertDialog.Builder(activity)
            .setTitle(R.string.action_backup)
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
            OpenDocumentContract.Config(arrayOf(mimeType))
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