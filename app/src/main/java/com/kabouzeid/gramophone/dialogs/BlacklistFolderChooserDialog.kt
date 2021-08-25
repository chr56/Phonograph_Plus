package com.kabouzeid.gramophone.dialogs

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import com.kabouzeid.gramophone.App
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.provider.BlacklistStore
import java.io.File

class BlacklistFolderChooserDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Storage permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ActivityCompat.checkSelfPermission(
                    requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE
                )
            != PackageManager.PERMISSION_GRANTED
        ) {
            return MaterialDialog(requireContext())
                .title(R.string.Permission_denied)
                .message(R.string.err_permission_storage) // TODO ResFile
                .positiveButton(android.R.string.ok)
        }

        // FileChooser
        val dialog = MaterialDialog(requireContext())
            .folderChooser(context = requireContext(), waitForPositiveButton = true, emptyTextRes = R.string.empty, initialDirectory = File("/storage/emulated/0")) {
                _, file ->
                MaterialDialog(requireContext())
                        .title(R.string.add_blacklist)// todo ResFile
                        .message(text = file.absolutePath)
                        .positiveButton(android.R.string.ok) {
                            BlacklistStore.getInstance(App.getInstance()).addPath(file)
                            dismiss() // dismiss alert dialog
                        }
                        .negativeButton(android.R.string.cancel) { dismiss() /* dismiss alert dialog */}
                        .show()
                dismiss() // dismiss Folder Chooser
            }
            .noAutoDismiss()
            .positiveButton(R.string.add_action )
            .negativeButton(android.R.string.cancel) { dismiss() }

            return dialog
    }
}
