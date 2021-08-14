package com.kabouzeid.gramophone.dialogs

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.kabouzeid.gramophone.R
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.arrayListOf
import kotlin.collections.toTypedArray

/**
 * @author Aidan Follestad (afollestad), modified by Karim Abou Zeid & chr_56
 */
class BlacklistFolderChooserDialog : DialogFragment() {
    private var parentFolder: File? = null
    private var parentContents: Array<File>? = null
    private var canGoUp = false
    private var callback: FolderCallback? = null
    var initialPath = Environment.getExternalStorageDirectory().absolutePath
    private val contentsList: List<CharSequence?>
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
            return MaterialDialog(requireActivity())
                .title(R.string.permissions_denied)
                .message(R.string.err_permission_storage)
                .positiveButton(android.R.string.ok) { dismiss() }
        }
        if (savedInstanceState == null) {
            savedInstanceState = Bundle()
        }
        if (!savedInstanceState.containsKey("current_path")) {
            savedInstanceState.putString("current_path", initialPath)
        }
        parentFolder = File(savedInstanceState.getString("current_path", File.pathSeparator))
        checkIfCanGoUp()
        parentContents = listFiles()
        val dialog: MaterialDialog = MaterialDialog(requireActivity())
            .title(text = parentFolder!!.absolutePath)
            .listItems(items = contentsList as List<CharSequence>)
            .noAutoDismiss()
            .positiveButton(R.string.add_action) { dialog ->
                dismiss()
                callback!!.onFolderSelection(this@BlacklistFolderChooserDialog, parentFolder!!)
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
        dialog.listItems(items = contentsList as List<CharSequence>)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_path", parentFolder!!.absolutePath)
    }

    fun setCallback(callback: FolderCallback?) {
        this.callback = callback
    }

    interface FolderCallback {
        fun onFolderSelection(dialog: BlacklistFolderChooserDialog, folder: File)
    }

    private class FolderSorter : Comparator<File> {
        override fun compare(lhs: File, rhs: File): Int {
            return lhs.name.compareTo(rhs.name)
        }
    }

    companion object {
        @JvmStatic
        fun create(): BlacklistFolderChooserDialog {
            return BlacklistFolderChooserDialog()
        }
    }
}
