/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.service.util

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.MediaStore
import player.phonograph.MusicServiceMsgConst
import player.phonograph.service.MusicService

class MediaStoreObserverUtil {

    var mediaStoreObserver: MediaStoreObserver? = null

    fun setUpMediaStoreObserver(
        context: Context,
        playerHandler: Handler, // todo
        handleAndSendChangeInternalCallback: (String) -> Unit
    ) {
        mediaStoreObserver = MediaStoreObserver(playerHandler, handleAndSendChangeInternalCallback)
        with(context) {
            contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver!!
            )
            contentResolver.registerContentObserver(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver!!
            )
            contentResolver.registerContentObserver(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver!!
            )
            contentResolver.registerContentObserver(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver!!
            )
            contentResolver.registerContentObserver(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver!!
            )

            contentResolver.registerContentObserver(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver!!
            )
            contentResolver.registerContentObserver(
                MediaStore.Audio.Albums.INTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver!!
            )
            contentResolver.registerContentObserver(
                MediaStore.Audio.Artists.INTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver!!
            )
            contentResolver.registerContentObserver(
                MediaStore.Audio.Genres.INTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver!!
            )
            contentResolver.registerContentObserver(
                MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI,
                true,
                mediaStoreObserver!!
            )
        }
    }

    fun unregisterMediaStoreObserver(context: Context) {
        context.contentResolver.unregisterContentObserver(mediaStoreObserver!!)
    }

    class MediaStoreObserver(
        private val mHandler: Handler,
        private val handleAndSendChangeInternalCallback: (String) -> Unit
    ) : ContentObserver(mHandler), Runnable {

        override fun onChange(selfChange: Boolean) {
            // if a change is detected, remove any scheduled callback
            // then post a new one. This is intended to prevent closely
            // spaced events from generating multiple refresh calls
            mHandler.removeCallbacks(this)
            mHandler.postDelayed(this, REFRESH_DELAY)
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
