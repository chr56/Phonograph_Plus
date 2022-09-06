package player.phonograph.dialogs

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.files.folderChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import java.io.File
import java.lang.ref.WeakReference

class ScanMediaFolderDialog : DialogFragment() {
    private lateinit var initial: File
    private lateinit var activityWeakReference: WeakReference<Activity>

    @SuppressLint("ObsoleteSdkInt")
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
                .neutralButton(R.string.grant_permission) {
                    Util.navigateToStorageSetting(requireActivity())
                }
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
            ) { _, file ->
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
                            coroutineToast(App.instance, R.string.nothing_to_scan)
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
        dialog.getActionButton(WhichButton.POSITIVE).updateTextColor(accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEGATIVE).updateTextColor(accentColor(requireActivity()))
        dialog.getActionButton(WhichButton.NEUTRAL).updateTextColor(accentColor(requireActivity()))
        return dialog
    }
}
