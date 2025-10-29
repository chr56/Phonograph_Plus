/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.App
import player.phonograph.model.Song
import player.phonograph.model.file.FileItem
import player.phonograph.model.file.MediaPath
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

    override suspend fun listFiles(context: Context, path: MediaPath): List<FileItem> =
        if (useLegacyListFile) {
            MediaStoreFileEntities.listFilesLegacy(context, path)
        } else {
            MediaStoreFileEntities.listFilesMediaStore(context, path)
        }


    suspend fun currentSongs(context: Context): List<Song> {
        val entities = currentFiles.value
        return entities.flatMap { item ->
            if (item.content is FileItem.SongContent) {
                item.content.song.asList()
            } else {
                Songs.searchByPath(context, item.path, false)
            }
        }
    }

}
