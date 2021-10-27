package player.phonograph.dialogs

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.files.folderChooser
import player.phonograph.R
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.ui.fragments.mainactivity.folders.FoldersFragment
import player.phonograph.ui.fragments.mainactivity.folders.FoldersFragment.ArrayListPathsAsyncTask
import player.phonograph.util.PreferenceUtil
import java.io.File
import java.lang.ref.WeakReference

class ScanMediaFolderDialog : DialogFragment() {
//    private lateinit var arg: Bundle
//    private lateinit var selected: File
//    private lateinit var initialPath: String
    private lateinit var initial: File
//    private var mode: Int = 0

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

        // init Default Path
        initial = PreferenceUtil.getInstance(requireContext()).getStartDirectory()
//        initialPath = PreferenceUtil.getInstance(requireContext()).startDirectory.absolutePath
//        var mSavedInstanceState = savedInstanceState
//        if (mSavedInstanceState == null) {
//            mSavedInstanceState = Bundle()
//        }
//        if (!savedInstanceState!!.containsKey("current_path")) {
//            mSavedInstanceState.putString("current_path", initialPath)
//        }

        // FileChooser
        val dialog = MaterialDialog(requireContext())
            .folderChooser(context = requireContext(), waitForPositiveButton = true, emptyTextRes = R.string.empty, initialDirectory = initial,) {
                _, file ->
//                selected = file
                val applicationContext = requireActivity().applicationContext
                val activityWeakReference = WeakReference<Activity?>(activity)
                Log.d(null, file.absolutePath)

                dismiss()
                ArrayListPathsAsyncTask(activity) { paths: Array<String>? ->
                    scanPaths(activityWeakReference, applicationContext, paths)
                }.execute(ArrayListPathsAsyncTask.LoadingInfo(file, FoldersFragment.AUDIO_FILE_FILTER))
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

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arg = requireArguments()
//    }

    companion object {
//        @JvmStatic
//        fun Create(mode: Int) {
//        }

        private fun scanPaths(
            activityWeakReference: WeakReference<Activity?>,
            applicationContext: Context,
            toBeScanned: Array<String>?
        ) {
            val activity = activityWeakReference.get()
            if (toBeScanned == null || toBeScanned.isEmpty()) {
                Toast.makeText(applicationContext, R.string.nothing_to_scan, Toast.LENGTH_SHORT).show()
            } else {
                MediaScannerConnection.scanFile(applicationContext, toBeScanned, null, activity?.let { UpdateToastMediaScannerCompletionListener(it, toBeScanned) })
            }
        }
    }
}
