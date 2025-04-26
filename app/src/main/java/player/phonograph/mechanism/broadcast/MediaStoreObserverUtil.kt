/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.broadcast

import legacy.phonograph.MediaStoreCompat.Audio.Playlists
import player.phonograph.App
import player.phonograph.mechanism.event.EventHub
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Artists
import android.provider.MediaStore.Audio.Genres
import android.provider.MediaStore.Audio.Media

private lateinit var mediaStoreObserver: MediaStoreObserver
private class MediaStoreObserver(
    private val playerHandler: Handler,
) : ContentObserver(playerHandler), Runnable {

    override fun onChange(selfChange: Boolean) {
        // if a change is detected, remove any scheduled callback
        // then post a new one. This is intended to prevent closely
        // spaced events from generating multiple refresh calls
        playerHandler.removeCallbacks(this)
        playerHandler.postDelayed(this, REFRESH_DELAY)
    }

    override fun run() {
        EventHub.sendEvent(App.instance, EventHub.EVENT_MEDIASTORE_CHANGED)
    }

    companion object {
        // milliseconds to delay before calling refresh to aggregate events
        private const val REFRESH_DELAY: Long = 500
    }
}


fun setUpMediaStoreObserver(context: Context, playerHandler: Handler) {
    mediaStoreObserver = MediaStoreObserver(playerHandler)
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
