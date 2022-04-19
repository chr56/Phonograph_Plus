/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.mainactivity.folders

import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import player.phonograph.notification.ErrorNotification
import player.phonograph.util.FileUtil
import java.io.File
import java.io.FileFilter
import java.lang.Exception

class FolderFragmentViewModel : ViewModel() {
    var isRecyclerViewPrepared: Boolean = false

    var listPathsJob: Job? = null
    private var onPathsListedCallback: ((Array<String?>) -> Unit)? = null
    fun listPaths(loadingInfos: LoadingInfo, onPathsListed: (Array<String?>) -> Unit) {
        onPathsListedCallback = onPathsListed
        listPathsJob = viewModelScope.launch(Dispatchers.IO) {

            val paths: Array<String?>
            if (!isActive) return@launch
            try {
                if (loadingInfos.file.isDirectory) {
                    val files = FileUtil.listFilesDeep(loadingInfos.file, loadingInfos.fileFilter)
                    if (!isActive) return@launch

                    paths = arrayOfNulls(files.size)
                    for (i in files.indices) {
                        if (!isActive) return@launch
                        val f = files[i]
                        paths[i] = FileUtil.safeGetCanonicalPath(f)
                    }
                } else {
                    paths = arrayOfNulls(1)
                    paths[0] = FileUtil.safeGetCanonicalPath(loadingInfos.file)
                }
            } catch (e: Exception) {
                ErrorNotification.postErrorNotification(e, "Fail to Load files!")
                e.printStackTrace()
                return@launch
            }
            withContext(Dispatchers.Main) {
                onPathsListedCallback?.invoke(paths)
            }
        }
    }

    override fun onCleared() {
        listPathsJob?.cancel()
        onPathsListedCallback = null
        super.onCleared()
    }

    companion object {
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
    class LoadingInfo(val file: File, val fileFilter: FileFilter)
}
