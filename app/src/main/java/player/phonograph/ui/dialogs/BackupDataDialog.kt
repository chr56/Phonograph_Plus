/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.migrate.DatabaseBackupManger
import player.phonograph.misc.ICreateFileStorageAccess
import player.phonograph.misc.IOpenFileStorageAccess
import player.phonograph.misc.OpenDocumentContract
import player.phonograph.util.TimeUtil
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.net.Uri
import android.os.Bundle

class BackupDataDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        require(activity is IOpenFileStorageAccess && activity is ICreateFileStorageAccess) {
            "Unsupported activity (SAF unavailable)"
        }
        val items = mapOf(
            0 to "export path filter",
            1 to "import path filter",
        )
        return AlertDialog.Builder(activity)
            .setTitle("Backup")
            .setItems(items.values.toTypedArray()) { dialog, i ->
                dialog.dismiss()
                when (i) {
                    0 -> {
                        activity.createFileStorageAccessTool.launch(
                            "phonograph_plus_path_filter_${TimeUtil.currentDateTime()}.json"
                        ) { uri: Uri? ->
                            uri ?: return@launch
                            DatabaseBackupManger.exportPathFilter(activity, uri)
                        }
                    }
                    1 -> {
                        activity.openFileStorageAccessTool.launch(
                            OpenDocumentContract.Cfg(null, arrayOf("application/json"))
                        ) { uri: Uri? ->
                            uri ?: return@launch
                            DatabaseBackupManger.importPathFilter(activity, uri)
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .create()
    }
}