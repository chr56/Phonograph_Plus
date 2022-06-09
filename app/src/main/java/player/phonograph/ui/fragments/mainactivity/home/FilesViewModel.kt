/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.*
import lib.phonograph.storage.externalStoragePath
import lib.phonograph.storage.getBasePath
import lib.phonograph.storage.getStorageId
import player.phonograph.App
import player.phonograph.mediastore.MediaStoreUtil
import player.phonograph.model.Song
import player.phonograph.settings.Setting

class FilesViewModel : ViewModel() {
    var currentLocation: Location = Location.HOME

    var currentFileList: MutableList<FileEntity> = ArrayList<FileEntity>(1)
        private set

    private var listFileJob: Job? = null
    fun loadFiles(location: Location = currentLocation, context: Context = App.instance) {
        listFileJob?.cancel() // cancel current
        listFileJob = viewModelScope.launch(Dispatchers.IO + SupervisorJob()) { listFiles(location, context, this) }
    }

    @Synchronized
    private fun listFiles(
        location: Location,
        context: Context = App.instance,
        scope: CoroutineScope?,
    ) {
        val paths = MediaStoreUtil.searchSongFiles(context, location.absolutePath) ?: return
        val list: MutableList<FileEntity> = ArrayList<FileEntity>(0)
        for (path in paths) {
            if (scope != null && !scope.isActive) return
            list.add(
                parsePath(currentLocation, path)
            )
        }
        currentFileList.clear()
        currentFileList.addAll(list)
    }

    companion object {
        fun parsePath(currentLocation: Location, absolutePath: String): FileEntity {
            val currentRelativePath = absolutePath.substringAfter(currentLocation.absolutePath).removePrefix("/")
            return if (currentRelativePath.contains('/')) {
                // folder
                FileEntity.Folder(
                    Location("${currentLocation.basePath}/${currentLocation.basePath.substringBefore('/')}", currentLocation.storageVolume)
                )
            } else {
                // file
                FileEntity.File(
                    Location("${currentLocation.basePath}/$currentLocation", currentLocation.storageVolume)
                )
            }
        }
    }

    /**
     * Presenting a File
     */
    sealed class FileEntity {
        abstract val path: Location

        class File(override val path: Location) : FileEntity() {
            val linkedSong: Song get() = MediaStoreUtil.getSong(App.instance, File(path.absolutePath)) ?: Song.EMPTY_SONG
            override val isFolder: Boolean = false
        }

        class Folder(override val path: Location) : FileEntity() {
            override val isFolder: Boolean = true
        }

        val name: String get() = path.basePath.takeLastWhile { it != '/' }
        abstract val isFolder: Boolean
    }

    /**
     * Presenting a path
     * @param basePath the path without prefix likes /storage/emulated/0 or /storage/69F4-242C,
     *  **starting with '/', ending without '/'**
     * @param storageVolume the location of Storage, such as Internal(`emulated/0`) or physical storage devices (`69F4-242C`)
     */
    class Location(val basePath: String, val storageVolume: String?) {
        val absolutePath: String
            get() {
                val prefix = if (storageVolume != null) "/storage/$storageVolume" else externalStoragePath
                return "$prefix$basePath"
            }

        companion object {

            /**
             * @param path absolute path
             */
            fun fromAbsolutePath(path: String, context: Context = App.instance): Location {
                val f = File(path)
                Log.d("Location", "From ${f.getBasePath(context)} @ ${f.getStorageId(context)}")
                return Location(f.getStorageId(context), f.getBasePath(context))
            }

            val HOME: Location = fromAbsolutePath(Setting.defaultStartDirectory.absolutePath)
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}
