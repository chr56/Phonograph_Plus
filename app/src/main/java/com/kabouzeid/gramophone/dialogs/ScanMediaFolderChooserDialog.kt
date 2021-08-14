package com.kabouzeid.gramophone.dialogs

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.misc.UpdateToastMediaScannerCompletionListener
import com.kabouzeid.gramophone.ui.fragments.mainactivity.folders.FoldersFragment
import com.kabouzeid.gramophone.ui.fragments.mainactivity.folders.FoldersFragment.ArrayListPathsAsyncTask
import com.kabouzeid.gramophone.util.PreferenceUtil
import java.io.File
import java.lang.ref.WeakReference
import java.util.Collections
import kotlin.Comparator
import kotlin.collections.ArrayList

// Todo
/**
 * @author Aidan Follestad (afollestad), modified by Karim Abou Zeid
 */
class ScanMediaFolderChooserDialog : DialogFragment() {
    private lateinit var initialPath : String
    private var parentFolder: File? = null
    private var parentContents: Array<File>? = null
    private var canGoUp = false
    private val contentsArray: List<CharSequence?>
        get() {
            if (parentContents == null) {
                return if (canGoUp) {
                    arrayListOf("..")
                } else arrayListOf()
            }
            val resultList = List<CharSequence?>(parentContents!!.size + if (canGoUp) 1 else 0) {
                if (canGoUp) {
                    if (it == 0) {
                        ".."
                    } else {
                        parentContents!![it - 1].name
                    }
                } else {
                    parentContents!![it].name
                }
            }
            return resultList
        }

    private fun listFiles(): Array<File>? {
        val contents = parentFolder!!.listFiles()
        val results: MutableList<File> = ArrayList()
        if (contents != null) {
            for (fi in contents) {
                if (fi.isDirectory) {
                    results.add(fi)
                }
            }
            Collections.sort(results, FolderSorter())
            return results.toTypedArray()
        }
        return null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var savedInstanceState = savedInstanceState
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ActivityCompat.checkSelfPermission(
                    requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE
                )
            != PackageManager.PERMISSION_GRANTED
        ) {
            return MaterialDialog(requireContext())
                .title(R.string.Permission_denied)
                .message(R.string.err_permission_storage) // TODO
                .positiveButton(android.R.string.ok)
        }
        initialPath = PreferenceUtil.getInstance(requireContext()).startDirectory.absolutePath
        if (savedInstanceState == null) {
            savedInstanceState = Bundle()
        }
        if (!savedInstanceState.containsKey("current_path")) {
            savedInstanceState.putString("current_path", initialPath)
        }
        parentFolder = File(savedInstanceState.getString("current_path", File.pathSeparator))
        checkIfCanGoUp()
        parentContents = listFiles()
        val dialog = MaterialDialog(requireContext())
            .title(text = parentFolder!!.absolutePath)
            .listItems(items = contentsArray as List<CharSequence>)
            .noAutoDismiss()
            .positiveButton(R.string.action_scan_directory) { dialog ->
                val applicationContext = requireActivity().applicationContext
                val activityWeakReference = WeakReference<Activity?>(activity)
                dismiss()
                ArrayListPathsAsyncTask(activity) { paths: Array<String>? -> scanPaths(activityWeakReference, applicationContext, paths) }.execute(ArrayListPathsAsyncTask.LoadingInfo(parentFolder, FoldersFragment.AUDIO_FILE_FILTER))
            }
            .negativeButton(android.R.string.cancel) { dismiss() }
        return dialog
    }

    fun onSelection(materialDialog: MaterialDialog?, view: View?, i: Int, s: CharSequence?) {
        if (canGoUp && i == 0) {
            parentFolder = parentFolder?.parentFile
            if (parentFolder?.absolutePath == "/storage/emulated") {
                parentFolder = parentFolder?.getParentFile()
            }
            checkIfCanGoUp()
        } else {
            parentFolder = parentContents!![if (canGoUp) i - 1 else i]
            canGoUp = true
            if (parentFolder!!.absolutePath == "/storage/emulated") {
                parentFolder = Environment.getExternalStorageDirectory()
            }
        }
        reload()
    }

    private fun checkIfCanGoUp() {
        canGoUp = parentFolder!!.parent != null
    }

    private fun reload() {
        parentContents = listFiles()
        val dialog = dialog as MaterialDialog?
        dialog!!.setTitle(parentFolder!!.absolutePath)
        dialog.listItems(items = contentsArray as List<CharSequence>)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_path", parentFolder!!.absolutePath)
    }

    private class FolderSorter : Comparator<File> {
        override fun compare(lhs: File, rhs: File): Int {
            return lhs.name.compareTo(rhs.name)
        }
    }

    companion object {
        @JvmStatic
        fun create(): ScanMediaFolderChooserDialog {
            return ScanMediaFolderChooserDialog()
        }

        private fun scanPaths(activityWeakReference: WeakReference<Activity?>, applicationContext: Context, toBeScanned: Array<String>?) {
            val activity = activityWeakReference.get()
            if (toBeScanned == null || toBeScanned.size < 1) {
                Toast.makeText(applicationContext, R.string.nothing_to_scan, Toast.LENGTH_SHORT).show()
            } else {
                MediaScannerConnection.scanFile(applicationContext, toBeScanned, null, activity?.let { UpdateToastMediaScannerCompletionListener(it, toBeScanned) })
            }
        }
    }
}
