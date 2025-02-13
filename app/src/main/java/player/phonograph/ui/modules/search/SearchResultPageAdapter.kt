/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.search

import player.phonograph.model.pages.Pages
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SearchResultPageAdapter(
    activity: FragmentActivity,
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = SearchType.entries.size

    override fun createFragment(position: Int): Fragment {
        return when (SearchType.entries[position]) {
            SearchType.SONGS     -> SongSearchResultPageFragment()
            SearchType.ALBUMS    -> AlbumSearchResultPageFragment()
            SearchType.ARTISTS   -> ArtistSearchResultPageFragment()
            SearchType.PLAYLISTS -> PlaylistSearchResultPageFragment()
            SearchType.GENRES    -> GenreSearchResultPageFragment()
            SearchType.QUEUE     -> QueueSearchResultPageFragment()
        }
    }

    fun lookup(name: String?): Int =
        when (name) {
            Pages.SONG     -> SearchType.SONGS.ordinal
            Pages.ALBUM    -> SearchType.ALBUMS.ordinal
            Pages.ARTIST   -> SearchType.ARTISTS.ordinal
            Pages.PLAYLIST -> SearchType.PLAYLISTS.ordinal
            Pages.GENRE    -> SearchType.GENRES.ordinal
            else           -> 0
        }

}