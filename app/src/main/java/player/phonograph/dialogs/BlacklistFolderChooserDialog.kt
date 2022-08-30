package player.phonograph.dialogs

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.files.folderChooser
import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.preferences.BlacklistPreferenceDialog
import player.phonograph.provider.BlacklistStore
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
                .message(R.string.err_permission_storage)
                .positiveButton(android.R.string.ok)
        }

        // FileChooser
        val dialog = MaterialDialog(requireContext())
            .folderChooser(context = requireContext(), waitForPositiveButton = true, emptyTextRes = R.string.empty, initialDirectory = File("/storage/emulated/0")) {
                _, file ->
                val alertDialog = MaterialDialog(requireContext())
                    .title(R.string.add_blacklist)
                    .message(text = file.absolutePath)

                alertDialog
                    .positiveButton(android.R.string.ok) {
                        BlacklistStore.getInstance(App.instance).addPath(file)
                        alertDialog.dismiss() // dismiss this alert dialog

                        dismiss() // dismiss Folder Chooser
                        BlacklistPreferenceDialog.newInstance().show(parentFragmentManager,"Blacklist_Preference_Dialog") // then reopen BlacklistPreferenceDialog
                    }
                    .negativeButton(android.R.string.cancel) {
                        alertDialog.dismiss() // dismiss this alert dialog
                    }
                    .show()
                // todo tint
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
