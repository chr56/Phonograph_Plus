package player.phonograph.dialogs

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.files.folderChooser
import java.io.File
import java.lang.ref.WeakReference
import kotlinx.coroutines.*
import player.phonograph.App
import player.phonograph.R
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting
import player.phonograph.ui.fragments.mainactivity.folders.DirectoryInfo
import player.phonograph.ui.fragments.mainactivity.folders.FileScanner
import player.phonograph.util.Util
import util.mdcolor.pref.ThemeColor

class ScanMediaFolderDialog : DialogFragment() {
    private lateinit var initial: File
    private lateinit var activityWeakReference: WeakReference<Activity>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activityWeakReference = WeakReference(requireActivity())

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

        // init Default Path
        initial = Setting.instance.startDirectory

        // FileChooser
        val dialog = MaterialDialog(requireContext())
            .folderChooser(
                context = requireContext(),
                waitForPositiveButton = true,
                emptyTextRes = R.string.empty,
                initialDirectory = initial,
            ) {
                    _, file ->
                dismiss()
                runCatching {
                    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                        val paths = FileScanner.listPaths(DirectoryInfo(file, FileScanner.audioFileFilter), this)
                        if (!paths.isNullOrEmpty()) {
                            withContext(Dispatchers.Main) {
                                val activity = activityWeakReference.get()
                                MediaScannerConnection.scanFile(
                                    activity ?: App.instance,
                                    paths,
                                    arrayOf("audio/*", "application/ogg", "application/x-ogg", "application/itunes"),
                                    if (activity != null) UpdateToastMediaScannerCompletionListener(activity, paths) else null
                                )
                            }
                        } else {
                            Util.coroutineToast(App.instance, R.string.nothing_to_scan)
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
        // set button color
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEUTRAL).updateTextColor(ThemeColor.accentColor(requireActivity()))
        return dialog
    }
}
