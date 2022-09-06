package player.phonograph.dialogs

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.files.folderChooser
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mt.pref.ThemeColor.accentColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting
import player.phonograph.util.CoroutineUtil.coroutineToast
import player.phonograph.util.FileUtil.DirectoryInfo
import player.phonograph.util.FileUtil.FileScanner
import player.phonograph.util.Util

class ScanMediaFolderDialog : DialogFragment() {
    private var initial: File = Setting.instance.startDirectory

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Storage permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            return MaterialDialog(requireContext())
                .title(R.string.Permission_denied)
                .message(R.string.err_permission_storage)
                .neutralButton(R.string.grant_permission) {
                    Util.navigateToStorageSetting(requireActivity())
                }
                .positiveButton(android.R.string.ok)
        }

        // FileChooser
        val dialog = MaterialDialog(requireContext())
            .folderChooser(
                context = requireContext(),
                waitForPositiveButton = true,
                emptyTextRes = R.string.empty,
                initialDirectory = initial,
            ) { _, file ->
                dismiss()
                runCatching {
                    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                        val paths = FileScanner.listPaths(DirectoryInfo(file, FileScanner.audioFileFilter), this)
                        if (!paths.isNullOrEmpty()) {
                            scan(activity, paths)
                        } else {
                            coroutineToast(requireContext().applicationContext, R.string.nothing_to_scan)
                        }
                    }
                }.apply {
                    if (isFailure) {
                        exceptionOrNull()?.let { e ->
                            ErrorNotification.postErrorNotification(e, "Scan Fail")
                            Log.w("ScanMediaFolderDialog", e)
                        }
                    }
                }
            }
            .noAutoDismiss()
            .positiveButton(android.R.string.ok)
            .negativeButton(android.R.string.cancel) { dismiss() }
            .apply {
                // set button color
                getActionButton(WhichButton.POSITIVE).updateTextColor(
                    accentColor(requireActivity())
                )
                getActionButton(WhichButton.NEGATIVE).updateTextColor(
                    accentColor(requireActivity())
                )
                getActionButton(WhichButton.NEUTRAL).updateTextColor(
                    accentColor(requireActivity())
                )
            }
        return dialog
    }

    companion object {
        fun scan(activity: Activity?, paths: Array<String>) {
            MediaScannerConnection.scanFile(
                activity?.applicationContext ?: App.instance,
                paths,
                arrayOf("audio/*", "application/ogg", "application/x-ogg", "application/itunes"),
                if (activity != null) UpdateToastMediaScannerCompletionListener(activity, paths) else null
            )
        }
    }
}
