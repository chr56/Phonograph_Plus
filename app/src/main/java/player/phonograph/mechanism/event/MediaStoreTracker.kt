/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.event

import player.phonograph.model.listener.MediaStoreChangedListener
import player.phonograph.util.warning
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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

    fun notifyAllListeners() = dispatch()

    fun dispatch() {
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

