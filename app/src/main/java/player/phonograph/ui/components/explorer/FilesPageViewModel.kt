/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import player.phonograph.mediastore.searchSongFiles
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.settings.Setting
import player.phonograph.util.FileUtil.FileScanner
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import java.io.File
import java.util.TreeSet

class FilesPageViewModel : AbsFileViewModel() {

    override fun listFiles(context: Context, location: Location, scope: CoroutineScope?): Set<FileEntity> {
        return if (useLegacyListFile) {
            listFilesLegacyImpl(location, scope)
        } else
            searchSongFiles(context, location, scope) ?: emptySet()
    }

    var useLegacyListFile: Boolean
        get() = Setting.instance.useLegacyListFilesImpl
        set(value) {
            Setting.instance.useLegacyListFilesImpl = value
        }

    var showFilesImages: Boolean
        get() = Setting.instance.showFileImages
        set(value) {
            Setting.instance.showFileImages = value
        }

    //todo
    private fun listFilesLegacyImpl(
        location: Location,
        scope: CoroutineScope?,
    ): Set<FileEntity> {
        val directory = File(location.absolutePath).also { if (!it.isDirectory) return emptySet() }
        val files = directory.listFiles(FileScanner.audioFileFilter) ?: return emptySet()
        val set = TreeSet<FileEntity>()
        for (file in files) {
            val l = Location.fromAbsolutePath(file.absolutePath)
            if (scope?.isActive == false) break
            val item =
                when {
                    file.isDirectory -> {
                        FileEntity.Folder(
                            location = l,
                            name = file.name,
                            dateAdded = file.lastModified(),
                            dateModified = file.lastModified()
                        ).also { it.songCount = 0 }
                    }

                    file.isFile      -> {
                        FileEntity.File(
                            location = l,
                            name = file.name,
                            size = file.length(),
                            dateAdded = file.lastModified(),
                            dateModified = file.lastModified()
                        )
                    }

                    else             -> null
                }
            item?.let { set.add(it) }
        }
        return set
    }
}
