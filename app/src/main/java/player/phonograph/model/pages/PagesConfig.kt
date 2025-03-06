/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.pages

data class PagesConfig(val tabs: List<String>) : List<String> by tabs {

    companion object {
        val DEFAULT_CONFIG = PagesConfig(
            mutableListOf(
                Pages.SONG,
                Pages.FOLDER,
                Pages.FILES,
                Pages.PLAYLIST,
                Pages.ALBUM,
                Pages.ARTIST,
                Pages.GENRE,
            )
        )
    }
}