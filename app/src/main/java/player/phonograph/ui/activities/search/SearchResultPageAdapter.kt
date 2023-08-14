/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities.search

import player.phonograph.R
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class SearchResultPageAdapter(
    searchActivity: SearchActivity,
) : FragmentStateAdapter(searchActivity) {

    enum class TabType(@StringRes val nameRes: Int) {
        SONG(R.string.song),
        ALBUM(R.string.album),
        ARTIST(R.string.artist),
        PLAYLIST(R.string.playlists),
        QUEUE(R.string.label_playing_queue);
    }

    override fun getItemCount(): Int = TabType.values().size

    override fun createFragment(position: Int): Fragment {
        return when (TabType.values()[position]) {
            TabType.SONG     -> SongSearchResultPageFragment()
            TabType.ALBUM    -> AlbumSearchResultPageFragment()
            TabType.ARTIST   -> ArtistSearchResultPageFragment()
            TabType.PLAYLIST -> PlaylistSearchResultPageFragment()
            TabType.QUEUE    -> QueueSearchResultPageFragment()
        }
    }

}