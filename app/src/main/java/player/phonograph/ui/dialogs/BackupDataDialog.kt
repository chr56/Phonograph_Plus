/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.dialogs

import player.phonograph.misc.ICreateFileStorageAccess
import player.phonograph.misc.IOpenFileStorageAccess
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.os.Bundle

class BackupDataDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        require(activity is IOpenFileStorageAccess && activity is ICreateFileStorageAccess) {
            "Unsupported activity (SAF unavailable)"
        }
        val items = mapOf(
            -1 to ""
        )
        return AlertDialog.Builder(activity)
            .setTitle("Backup")
            .setItems(items.values.toTypedArray()) { dialog, i ->
                dialog.dismiss()
                when (i) {
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .create()
    }
}