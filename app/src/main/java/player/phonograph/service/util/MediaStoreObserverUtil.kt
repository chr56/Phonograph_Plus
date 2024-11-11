/*
 * Copyright (c) 2022-2024 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.service.util

import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import player.phonograph.MusicServiceMsgConst
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Artists
import android.provider.MediaStore.Audio.Genres
import android.provider.MediaStore.Audio.Media

class MediaStoreObserverUtil {

    lateinit var mediaStoreObserver: MediaStoreObserver

    fun setUpMediaStoreObserver(
        context: Context,
        playerHandler: Handler,
        handleAndSendChangeInternalCallback: (String) -> Unit,
    ) {
        mediaStoreObserver = MediaStoreObserver(playerHandler, handleAndSendChangeInternalCallback)
        with(context.contentResolver) {
            registerContentObserver(Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Albums.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Artists.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Genres.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Playlists.EXTERNAL_CONTENT_URI, true, mediaStoreObserver)

            registerContentObserver(Media.INTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Albums.INTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Artists.INTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Genres.INTERNAL_CONTENT_URI, true, mediaStoreObserver)
            registerContentObserver(Playlists.INTERNAL_CONTENT_URI, true, mediaStoreObserver)
        }
    }

    fun unregisterMediaStoreObserver(context: Context) {
        context.contentResolver.unregisterContentObserver(mediaStoreObserver)
    }

    class MediaStoreObserver(
        private val playerHandler: Handler,
        private val handleAndSendChangeInternalCallback: (String) -> Unit,
    ) : ContentObserver(playerHandler), Runnable {

        override fun onChange(selfChange: Boolean) {
            // if a change is detected, remove any scheduled callback
            // then post a new one. This is intended to prevent closely
            // spaced events from generating multiple refresh calls
            playerHandler.removeCallbacks(this)
            playerHandler.postDelayed(this, REFRESH_DELAY)
        }

        override fun run() {
            // actually call refresh when the delayed callback fires
            // do not send a sticky broadcast here
            handleAndSendChangeInternalCallback(MusicServiceMsgConst.MEDIA_STORE_CHANGED)
        }

        companion object {
            // milliseconds to delay before calling refresh to aggregate events
            private const val REFRESH_DELAY: Long = 500
        }
    }
}
