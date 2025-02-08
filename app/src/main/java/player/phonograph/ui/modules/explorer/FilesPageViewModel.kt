/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.App
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.loaders.FileEntityLoader
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

class FilesPageViewModel : AbsFileViewModel() {

    var useLegacyListFile: Boolean
        get() = Setting(App.instance)[Keys.useLegacyListFilesImpl].data
        set(value) {
            Setting(App.instance)[Keys.useLegacyListFilesImpl].data = value
        }

    var showFilesImages: Boolean
        get() = Setting(App.instance)[Keys.showFileImages].data
        set(value) {
            Setting(App.instance)[Keys.showFileImages].data = value
        }

    override suspend fun listFiles(context: Context, location: Location, scope: CoroutineScope?): List<FileEntity> =
        if (useLegacyListFile) {
            FileEntityLoader.listFilesLegacy(location, scope)
        } else {
            FileEntityLoader.listFilesMediaStore(location, context, scope)
        }


    suspend fun currentSongs(context: Context): List<Song> {
        val entities = currentFiles.value
        return coroutineScope {
            val files = listFilesRecursively(context, entities, this)
            files.map { Songs.searchByFileEntity(context, it) }
        }
    }

    private suspend fun listFilesRecursively(
        context: Context,
        fileEntities: Collection<FileEntity>,
        scope: CoroutineScope?,
    ): List<FileEntity.File> = fileEntities.flatMap { fileEntity ->
        when (fileEntity) {
            is FileEntity.File   -> listOf(fileEntity)
            is FileEntity.Folder -> {
                val subEntities = listFiles(context, fileEntity.location, scope)
                listFilesRecursively(context, subEntities, scope)
            }
        }
    }

}
