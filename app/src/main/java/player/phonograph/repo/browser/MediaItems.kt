/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.repo.browser

import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
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
            .setMediaId(MediaItemPath.song(id).mediaId)
            .build(),
        FLAG_PLAYABLE
    )

fun QueueSong.toMediaItem(): MediaItem =
    MediaItem(
        MediaDescriptionCompat.Builder()
            .setTitle(song.title)
            .setSubtitle(song.albumName)
            .setDescription(song.artistName)
            .setMediaId(MediaItemPath.queueSong(index).mediaId)
            .build(),
        FLAG_PLAYABLE
    )

fun Album.toMediaItem(): MediaItem =
    MediaItem(
        MediaDescriptionCompat.Builder()
            .setTitle(title)
            .setSubtitle(artistName)
            .setDescription(artistName)
            .setMediaId(MediaItemPath.album(id).mediaId)
            .build(),
        FLAG_BROWSABLE
    )

fun Artist.toMediaItem(): MediaItem =
    MediaItem(
        MediaDescriptionCompat.Builder()
            .setTitle(name)
            .setMediaId(MediaItemPath.artist(id).mediaId)
            .build(),
        FLAG_BROWSABLE
    )

