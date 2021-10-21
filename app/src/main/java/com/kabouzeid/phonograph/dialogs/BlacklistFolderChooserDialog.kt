package com.kabouzeid.phonograph.dialogs

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.files.folderChooser
import com.kabouzeid.phonograph.App
import com.kabouzeid.phonograph.R
import com.kabouzeid.phonograph.provider.BlacklistStore
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
                val dialog = MaterialDialog(requireContext())
                    .title(R.string.add_blacklist) // todo ResFile
                    .message(text = file.absolutePath)
                    .positiveButton(android.R.string.ok) {
                        BlacklistStore.getInstance(App.instance).addPath(file)
                        dismiss() // dismiss alert dialog
                    }
                    .negativeButton(android.R.string.cancel) { dismiss() /* dismiss alert dialog */ }
                    .show()
                // todo tint
                dismiss() // dismiss Folder Chooser
            }
            .noAutoDismiss()
            .positiveButton(R.string.add_action)
            .negativeButton(android.R.string.cancel) { dismiss() }
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))

        return dialog
    }
}
