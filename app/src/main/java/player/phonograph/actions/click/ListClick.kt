/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions.click

import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Displayable
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.settings.Setting
import player.phonograph.util.NavigationUtil
import androidx.core.util.Pair
import android.app.Activity
import android.widget.ImageView

/**
 * involve item click
 * @param list      a list that this Displayable is among
 * @param position  position where selected
 * @param activity  (optional) for SceneTransitionAnimation
 * @param imageView (optional) item's imagine for SceneTransitionAnimation
 * @return true if action have been processed
 */
fun <T : Displayable> listClick(
    list: List<T>,
    position: Int,
    activity: Activity?,
    imageView: ImageView?,
): Boolean {
    if (list.isEmpty()) return false
    when (list.firstOrNull()) {
        is Song     -> {
            val base = Setting.instance.songItemClickMode
            val extra = Setting.instance.songItemClickExtraFlag
            songClick(list.filterIsInstance<Song>(), position, base, extra)
        }
        is Album    -> {
            if (activity != null) {
                if (imageView != null) {
                    NavigationUtil.goToAlbum(
                        activity,
                        (list[position] as Album).id,
                        Pair(
                            imageView,
                            imageView.resources.getString(R.string.transition_album_art)
                        )
                    )
                } else {
                    NavigationUtil.goToAlbum(
                        activity,
                        (list[position] as Album).id
                    )
                }
            } else {
                return false
            }
        }
        is Artist   -> {
            if (activity != null) {
                if (imageView != null) {
                    NavigationUtil.goToArtist(
                        activity,
                        (list[position] as Artist).id,
                        Pair(
                            imageView,
                            imageView.resources.getString(R.string.transition_artist_image)
                        )
                    )
                } else {
                    NavigationUtil.goToArtist(activity, (list[position] as Artist).id)
                }

            } else {
                return false
            }
        }
        is Genre    -> {
            if (activity != null) {
                NavigationUtil.goToGenre(activity, (list[position] as Genre))
            } else {
                return false
            }
        }
        is Playlist -> {
            if (activity != null) {
                NavigationUtil.goToPlaylist(activity, list[position] as Playlist)
            } else {
                return false
            }
        }
        else        -> return false
    }
    return true
}