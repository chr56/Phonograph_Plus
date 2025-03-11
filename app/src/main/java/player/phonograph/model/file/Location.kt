/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.file

import android.os.storage.StorageVolume

/**
 * Presenting a path
 */
interface Location {

    /**
     * relative path without prefix likes `/storage/emulated/0` or `/storage/69F4-242C`,
     * **starting with '/', ending without '/'**
     */
    val basePath: String


    /**
     * absolute path from filesystem root
     */
    val absolutePath: String

    /**
     * storageVolume StorageVolume where file locate
     */
    val storageVolume: StorageVolume

    /**
     *  null if no parent (root location)
     */
    val parent: Location?

}