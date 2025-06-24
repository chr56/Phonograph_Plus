/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.repo.mediastore.MediaStoreFileEntities
import android.content.Context

class FilesChooserViewModel : AbsFileViewModel() {

    override suspend fun listFiles(context: Context, location: Location): List<FileEntity> =
        MediaStoreFileEntities.listFilesLegacy(context, location)
}
