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
            MediaStoreFileEntities.listFilesLegacy(location, context, scope)
        } else {
            MediaStoreFileEntities.listFilesMediaStore(location, context, scope)
        }


    suspend fun currentSongs(context: Context): List<Song> {
        val entities = currentFiles.value
        return coroutineScope {
            entities.flatMap {
                when (it) {
                    is FileEntity.File   -> Songs.id(context, it.id).asList()
                    is FileEntity.Folder -> Songs.searchByPath(context, it.location.absolutePath, false)
                }
            }
        }
    }

}
