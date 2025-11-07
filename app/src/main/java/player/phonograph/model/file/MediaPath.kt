/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.file

/**
 * Present path of a (Media) file or directory
 */
interface MediaPath {

    /**
     * normalized absolute path of this file or directory
     */
    val path: String

    /**
     * MediaStore ID, -1 if it is a directory or not in MediaStore
     */
    val mediastoreId: Long

    /**
     * current StorageVolume of this file or directory
     */
    val volume: Volume

    /**
     * See [Volume.root]
     */
    val volumeRoot: String

    /**
     * relative path from [volumeRoot],
     * **starting with '/', ending without '/'**
     */
    val basePath: String

    /**
     * relative path segments from [volumeRoot],
     */
    val basePathSegments: List<String>

    /**
     * true if it is root directory of current StorageVolume
     */
    val isRoot: Boolean

    /**
     * Presents a StorageVolume
     */
    interface Volume {

        /**
         * StorageVolume UUID
         */
        val uuid: String

        /**
         * StorageVolume name
         */
        val name: String

        /**
         * presents this StorageVolume if it's primary volume
         */
        val isPrimary: Boolean

        /**
         * root path of current StorageVolume
         * (it should be like `/storage/emulated/0` or `/storage/69F4-242C`)
         */
        val root: String

    }
}