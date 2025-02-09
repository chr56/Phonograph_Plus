/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.search

import player.phonograph.R
import player.phonograph.model.pages.Pages
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SearchResultPageAdapter(
    activity: FragmentActivity,
) : FragmentStateAdapter(activity) {

    enum class TabType(@param:StringRes val nameRes: Int) {
        SONG(R.string.label_songs),
        ALBUM(R.string.label_albums),
        ARTIST(R.string.label_artists),
        PLAYLIST(R.string.label_playlists),
        GENRES(R.string.label_genres),
        QUEUE(R.string.label_playing_queue);
    }

    override fun getItemCount(): Int = TabType.entries.size

    override fun createFragment(position: Int): Fragment {
        return when (TabType.entries[position]) {
            TabType.SONG     -> SongSearchResultPageFragment()
            TabType.ALBUM    -> AlbumSearchResultPageFragment()
            TabType.ARTIST   -> ArtistSearchResultPageFragment()
            TabType.PLAYLIST -> PlaylistSearchResultPageFragment()
            TabType.GENRES   -> GenreSearchResultPageFragment()
            TabType.QUEUE    -> QueueSearchResultPageFragment()
        }
    }

    fun lookup(name: String?): Int =
        when (name) {
            Pages.SONG     -> TabType.SONG.ordinal
            Pages.ALBUM    -> TabType.ALBUM.ordinal
            Pages.ARTIST   -> TabType.ARTIST.ordinal
            Pages.PLAYLIST -> TabType.PLAYLIST.ordinal
            Pages.GENRE    -> TabType.GENRES.ordinal
            else           -> 0
        }

}