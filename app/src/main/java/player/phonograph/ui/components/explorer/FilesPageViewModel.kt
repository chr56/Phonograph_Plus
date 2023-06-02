/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.Location
import player.phonograph.settings.Setting
import android.content.Context
import kotlinx.coroutines.CoroutineScope

class FilesPageViewModel : AbsFileViewModel() {

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

    override fun listFiles(context: Context, location: Location, scope: CoroutineScope?): Set<FileEntity> {
        return if (useLegacyListFile) {
            listFilesLegacyImpl(location, scope)
        } else
            listFilesMediaStoreImpl(location, context, scope)
    }

}
