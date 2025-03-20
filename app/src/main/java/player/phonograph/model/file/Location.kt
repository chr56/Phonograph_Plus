/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.model.file

/**
 * Presenting a path
 */
interface Location {

    /**
     * absolute path from filesystem root
     */
    val absolutePath: String

    /**
     * StorageVolume UUID where this locates
     */
    val volumeUUID: String

    /**
     * StorageVolume name where this locates
     */
    val volumeName: String

    /**
     * root path of current StorageVolume where this locates
     * (it should be like `/storage/emulated/0` or `/storage/69F4-242C`)
     */
    val volumeRootPath: String

    /**
     * relative path from [volumeRootPath],
     * **starting with '/', ending without '/'**
     */
    val basePath: String

    /**
     * true if this is root of current StorageVolume
     */
    val isRoot: Boolean

}