/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.event

import player.phonograph.MusicServiceMsgConst
import player.phonograph.model.listener.MediaStoreChangedListener
import player.phonograph.util.debug
import player.phonograph.util.warning
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import java.lang.ref.WeakReference


object MediaStoreTracker {
    private const val TAG = "MediaStoreTracker"


    private val listeners: MutableList<WeakReference<MediaStoreChangedListener>> = mutableListOf()

    fun register(listener: MediaStoreChangedListener) {
        listeners.add(WeakReference(listener))
    }

    fun unregister(listener: MediaStoreChangedListener) {
        listeners.remove(WeakReference(listener))
            .also { if (!it) warning(TAG, "${listener.javaClass} is not registered yet") }
    }

    fun unregisterAll() {
        listeners.clear()
    }

    private var initialed = false
    private lateinit var mediaStoreReceiver: MediaStoreReceiver
    fun setup(context: Context) {
        if (!initialed) {
            mediaStoreReceiver = MediaStoreReceiver()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(
                    mediaStoreReceiver,
                    IntentFilter(MusicServiceMsgConst.MEDIA_STORE_CHANGED),
                    RECEIVER_NOT_EXPORTED
                )
            } else {
                context.applicationContext.registerReceiver(
                    mediaStoreReceiver,
                    IntentFilter(MusicServiceMsgConst.MEDIA_STORE_CHANGED)
                )
            }
            initialed = true
        } else {
            debug { Log.i(TAG, "Already initialed") }
        }
    }

    private fun dispatch() {
        val rmBin = mutableListOf<WeakReference<MediaStoreChangedListener>>()
        for (weakReference in listeners) {
            val listener = weakReference.get()
            if (listener != null) {
                listener.onMediaStoreChanged()
            } else {
                rmBin.add(weakReference)
            }
        }
        listeners.removeAll(rmBin)
    }

    private class MediaStoreReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == MusicServiceMsgConst.MEDIA_STORE_CHANGED) {
                dispatch()
            }
        }
    }

    abstract class LifecycleListener : DefaultLifecycleObserver, MediaStoreChangedListener {
        override fun onCreate(owner: LifecycleOwner) {
            super.onDestroy(owner)
            register(this)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            unregister(this)
        }
    }
}

