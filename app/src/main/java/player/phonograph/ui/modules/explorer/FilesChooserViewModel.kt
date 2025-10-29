/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.model.file.FileItem
import player.phonograph.model.file.MediaPath
import player.phonograph.repo.mediastore.MediaStoreFileEntities
import android.content.Context

class FilesChooserViewModel : AbsFileViewModel() {

    override suspend fun listFiles(context: Context, path: MediaPath): List<FileItem> =
        MediaStoreFileEntities.listFilesLegacy(context, path)
}
