/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.App
import player.phonograph.R
import player.phonograph.mechanism.migrate.DatabaseBackupManger
import player.phonograph.mechanism.migrate.SettingDataManager
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenDocumentContract
import player.phonograph.util.coroutineToast
import player.phonograph.util.text.currentDateTime
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

        val i = resources.getString(R.string.action_import)
        val e = resources.getString(R.string.action_export)


        val importSetting = String.format(i, resources.getString(R.string.action_settings))
        val importPathFilter = String.format(i, resources.getString(R.string.path_filter))
        val importPlayingQueue = String.format(i, resources.getString(R.string.label_playing_queue))
        val importFavorite = String.format(i, resources.getString(R.string.favorites))

        val exportSetting = String.format(e, resources.getString(R.string.action_settings))
        val exportPathFilter = String.format(e, resources.getString(R.string.path_filter))
        val exportPlayingQueue = String.format(e, resources.getString(R.string.label_playing_queue))
        val exportFavorite = String.format(e, resources.getString(R.string.favorites))

        val menu = listOf(
            exportSetting to {
                export(
                    requireActivity(),
                    "phonograph_plus_settings_${currentDateTime()}.json"
                ) { uri ->
                    SettingDataManager.exportSettings(uri, activity)
                    true
                }
            },
            importSetting to {
                import(requireActivity(), "application/json") { uri ->
                    SettingDataManager.importSetting(uri, activity)
                    true
                }
            },
            exportPathFilter to {
                export(
                    requireActivity(),
                    "phonograph_plus_path_filter_${currentDateTime()}.json"
                ) { uri ->
                    DatabaseBackupManger.exportPathFilter(activity, uri)
                    true
                }
            },
            importPathFilter to {
                import(requireActivity(), "application/json") { uri ->
                    DatabaseBackupManger.importPathFilter(activity, uri)
                    true
                }
            },
            exportPlayingQueue to {
                export(
                    requireActivity(),
                    "phonograph_plus_playing_queues_${currentDateTime()}.json"
                ) { uri ->
                    DatabaseBackupManger.exportPlayingQueues(activity, uri)
                    true
                }
            },
            importPlayingQueue to {
                import(requireActivity(), "application/json") { uri ->
                    DatabaseBackupManger.importPlayingQueues(activity, uri)
                    true
                }
            },
            exportFavorite to {
                export(
                    requireActivity(),
                    "phonograph_plus_favorites_${currentDateTime()}.json"
                ) { uri ->
                    DatabaseBackupManger.exportFavorites(activity, uri)
                    true
                }
            },
            importFavorite to {
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
        coroutineToast(App.instance, if (this) R.string.success else R.string.failed)
    }
}