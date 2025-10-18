/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.mechanism.event

import player.phonograph.App
import player.phonograph.foundation.compat.MediaStoreCompat.Audio.Playlists
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.content.Context
import android.database.ContentObserver
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Artists
import android.provider.MediaStore.Audio.Genres
import android.provider.MediaStore.Audio.Media
import java.util.concurrent.atomic.AtomicInteger

object MediaStoreObservation {

    private var rc: AtomicInteger = AtomicInteger(0)
    private var observer: MediaStoreObserver? = null

    /**
     * Register MediaStoreObserver the [ContentObserver] if available.
     * It should be called on creating for a lifecycle object.
     */
    fun registerMediaStoreObserver(context: Context) {
        val application = context.applicationContext as App
        synchronized(rc) {
            if (rc.getAndIncrement() == 0) {
                observer = MediaStoreObserver(application).apply { register(application) }
            }
        }
    }

    /**
     * Unregister MediaStoreObserver the [ContentObserver] if available.
     * It should be called on destroying for a lifecycle object.
     */
    fun unregisterMediaStoreObserver(context: Context) {
        val application = context.applicationContext as App
        synchronized(rc) {
            if (rc.decrementAndGet() <= 0) {
                observer?.unregister(application)
                observer = null
            }
        }
    }

    /**
     * The [LifecycleObserver] for observation of MediaStore.
     */
    class LifecycleObserver : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            val context = (owner as? Context) ?: App.instance
            registerMediaStoreObserver(context)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            val context = (owner as? Context) ?: App.instance
            unregisterMediaStoreObserver(context)
        }
    }


    /**
     * Aggregate all MediaStore content changed trigger callbacks
     * into [EventHub.EVENT_MEDIASTORE_CHANGED] etc
     */
    private class MediaStoreObserver(private val application: App) :
            ContentObserver(application.appHandler), Runnable {

        override fun onChange(selfChange: Boolean) {
            // if a change is detected, remove any scheduled callback
            // then post a new one. This is intended to prevent closely
            // spaced events from generating multiple refresh calls
            application.appHandler.removeCallbacks(this)
            application.appHandler.postDelayed(this, REFRESH_DELAY)
        }

        override fun run() {
            EventHub.sendEvent(application, EventHub.EVENT_MEDIASTORE_CHANGED)
            EventHub.sendEvent(application, EventHub.EVENT_MUSIC_LIBRARY_CHANGED)
        }

        /**
         * register self as [ContentObserver]
         */
        fun register(context: Context) {
            with(context.contentResolver) {
                registerContentObserver(Media.EXTERNAL_CONTENT_URI, true, this@MediaStoreObserver)
                registerContentObserver(Albums.EXTERNAL_CONTENT_URI, true, this@MediaStoreObserver)
                registerContentObserver(Artists.EXTERNAL_CONTENT_URI, true, this@MediaStoreObserver)
                registerContentObserver(Genres.EXTERNAL_CONTENT_URI, true, this@MediaStoreObserver)
                registerContentObserver(Playlists.EXTERNAL_CONTENT_URI, true, this@MediaStoreObserver)

                registerContentObserver(Media.INTERNAL_CONTENT_URI, true, this@MediaStoreObserver)
                registerContentObserver(Albums.INTERNAL_CONTENT_URI, true, this@MediaStoreObserver)
                registerContentObserver(Artists.INTERNAL_CONTENT_URI, true, this@MediaStoreObserver)
                registerContentObserver(Genres.INTERNAL_CONTENT_URI, true, this@MediaStoreObserver)
                registerContentObserver(Playlists.INTERNAL_CONTENT_URI, true, this@MediaStoreObserver)
            }
        }

        /**
         * unregister self
         */
        fun unregister(context: Context) {
            with(context.contentResolver) {
                unregisterContentObserver(this@MediaStoreObserver)
            }
        }

        companion object {
            // milliseconds to delay before calling refresh to aggregate events
            private const val REFRESH_DELAY: Long = 500
        }
    }

}