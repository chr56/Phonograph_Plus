/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import android.content.Context
import kotlinx.coroutines.CoroutineScope

class FilesChooserViewModel : AbsFileViewModel() {

    override fun listFiles(context: Context, location: Location, scope: CoroutineScope?): Set<FileEntity> =
        listFilesLegacy(location, scope)
}
