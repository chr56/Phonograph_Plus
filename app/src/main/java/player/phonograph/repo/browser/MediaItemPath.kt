/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

data class MediaItemPath(val segments: List<String>, val parameters: Map<String, String>?) {

    companion object {
        internal const val ROOT_PATH = "/"
    }
}