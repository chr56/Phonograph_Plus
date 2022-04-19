/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.mainactivity.folders

import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import player.phonograph.notification.ErrorNotification
import player.phonograph.util.FileUtil
import player.phonograph.views.BreadCrumbLayout
import java.io.File
import java.io.FileFilter
import java.util.*
import kotlin.collections.ArrayList

class FolderFragmentViewModel : ViewModel() {
    var isRecyclerViewPrepared: Boolean = false

    var listPathsJob: Job? = null
    private var onPathsListedCallback: ((Array<String?>) -> Unit)? = null
    fun listPaths(loadingInfos: LoadingInfo, onPathsListed: (Array<String?>) -> Unit) {
        onPathsListedCallback = onPathsListed
        listPathsJob = viewModelScope.launch(Dispatchers.IO) {

            val paths = FileScanner.scanPaths(loadingInfos, this)

            withContext(Dispatchers.Main) {
                if (paths != null)
                    onPathsListedCallback?.invoke(paths)
            }
        }
    }

    var loadFilesJob: Job? = null
    private var onFilesReadyCallback: ((List<File>) -> Unit)? = null
    fun loadFiles(crumb: BreadCrumbLayout.Crumb?, onFilesReady: (List<File>) -> Unit) {
        onFilesReadyCallback = onFilesReady
        loadFilesJob = viewModelScope.launch(Dispatchers.IO) {
            val directory: File? = crumb?.file
            val files =
                if (directory != null) {
                    val files: MutableList<File> = FileUtil.listFiles(directory, FoldersFragment.AUDIO_FILE_FILTER)
                    if (!isActive) return@launch
                    Collections.sort(files, fileComparator)
                    files
                } else {
                    ArrayList()
                }
            if (!isActive) return@launch
            withContext(Dispatchers.Main) {
                onFilesReadyCallback?.invoke(files)
            }
        }
    }

    val fileComparator: Comparator<File> by lazy {
        Comparator { lhs: File, rhs: File ->
            if (lhs.isDirectory && !rhs.isDirectory) {
                return@Comparator -1
            } else if (!lhs.isDirectory && rhs.isDirectory) {
                return@Comparator 1
            } else {
                return@Comparator lhs.name.compareTo(rhs.name, ignoreCase = true)
            }
        }
    }

    override fun onCleared() {
        listPathsJob?.cancel()
        onPathsListedCallback = null
        loadFilesJob?.cancel()
        super.onCleared()
    }
}

class LoadingInfo(val file: File, val fileFilter: FileFilter)

object FileScanner {
    fun scanPaths(loadingInfos: LoadingInfo, scope: CoroutineScope): Array<String?>? {
        val paths: Array<String?>
        if (!scope.isActive) return null
        try {
            if (loadingInfos.file.isDirectory) {
                val files = FileUtil.listFilesDeep(loadingInfos.file, loadingInfos.fileFilter)
                if (!scope.isActive) return null

                paths = arrayOfNulls(files.size)
                for (i in files.indices) {
                    if (!scope.isActive) return null
                    val f = files[i]
                    paths[i] = FileUtil.safeGetCanonicalPath(f)
                }
            } else {
                paths = arrayOfNulls(1)
                paths[0] = FileUtil.safeGetCanonicalPath(loadingInfos.file)
            }
            Log.v("FileScanner", "success")
        } catch (e: Exception) {
            ErrorNotification.postErrorNotification(e, "Fail to Load files!")
            e.printStackTrace()
            return null
        }
        return paths
    }

    @JvmField
    val audioFileFilter: FileFilter =
        FileFilter { file: File ->
            !file.isHidden && (
                file.isDirectory ||
                    FileUtil.fileIsMimeType(file, "audio/*", MimeTypeMap.getSingleton()) ||
                    FileUtil.fileIsMimeType(file, "application/ogg", MimeTypeMap.getSingleton())
                )
        }
}
