/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.App
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.mediastore.MediaStoreFileEntities
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.util.asList
import android.content.Context

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

    override suspend fun listFiles(context: Context, location: Location): List<FileEntity> =
        if (useLegacyListFile) {
            MediaStoreFileEntities.listFilesLegacy(context, location)
        } else {
            MediaStoreFileEntities.listFilesMediaStore(context, location)
        }


    suspend fun currentSongs(context: Context): List<Song> {
        val entities = currentFiles.value
        return entities.flatMap {
            when (it) {
                is FileEntity.File   -> Songs.id(context, it.id).asList()
                is FileEntity.Folder -> Songs.searchByPath(context, it.location.absolutePath, false)
            }
        }
    }

}
