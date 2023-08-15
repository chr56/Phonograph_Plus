/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import android.content.res.Resources
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat

fun Song.toMediaItem(): MediaItem =
    MediaItem(
        MediaDescriptionCompat.Builder()
            .setTitle(title)
            .setSubtitle(albumName)
            .setDescription(artistName)
            .setMediaId("$MEDIA_BROWSER_SONGS$MEDIA_BROWSER_SEPARATOR$id")
            .build(),
        FLAG_PLAYABLE
    )

fun Album.toMediaItem(): MediaItem =
    MediaItem(
        MediaDescriptionCompat.Builder()
            .setTitle(title)
            .setSubtitle(artistName)
            .setDescription(artistName)
            .setMediaId("$MEDIA_BROWSER_ALBUMS$MEDIA_BROWSER_SEPARATOR$id")
            .build(),
        FLAG_BROWSABLE
    )

fun Artist.toMediaItem(): MediaItem =
    MediaItem(
        MediaDescriptionCompat.Builder()
            .setTitle(name)
            .setMediaId("$MEDIA_BROWSER_ARTISTS$MEDIA_BROWSER_SEPARATOR$id")
            .build(),
        FLAG_BROWSABLE
    )

fun albumShuffleItem(resources: Resources, id: Long): MediaItem =
    generalItem(
        resources.getString(R.string.action_shuffle_all),
        "$MEDIA_BROWSER_ALBUMS$MEDIA_BROWSER_SEPARATOR$id"
    )


fun artistShuffleItem(resources: Resources, id: Long): MediaItem =
    generalItem(
        resources.getString(R.string.action_shuffle_all),
        "$MEDIA_BROWSER_ARTISTS$MEDIA_BROWSER_SEPARATOR$id"
    )


private fun generalItem(title: String, path: String): MediaItem =
    MediaItem(
        MediaDescriptionCompat.Builder()
            .setTitle(title)
            .setMediaId(path)
            .build(),
        FLAG_PLAYABLE
    )