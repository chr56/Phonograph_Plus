/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.mechanism.event

import org.koin.core.context.GlobalContext
import player.phonograph.MusicServiceMsgConst
import player.phonograph.model.listener.MediaStoreChangedListener
import player.phonograph.util.registerReceiverCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.lang.ref.WeakReference


class MediaStoreTracker(context: Context) {

    init {
        EventReceiver.setupEventReceiver(context)
    }

    private val listeners: MutableList<WeakReference<MediaStoreChangedListener>> = mutableListOf()

    fun register(listener: MediaStoreChangedListener) {
        listeners.add(WeakReference(listener))
    }

    fun unregister(listener: MediaStoreChangedListener) {
        listeners.remove(WeakReference(listener))
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
        private val mediaStoreTracker: MediaStoreTracker by GlobalContext.get().inject()
        override fun onCreate(owner: LifecycleOwner) {
            super.onDestroy(owner)
            mediaStoreTracker.register(this)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            mediaStoreTracker.unregister(this)
        }
    }

    class EventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MusicServiceMsgConst.MEDIA_STORE_CHANGED -> GlobalContext.get().get<MediaStoreTracker>().dispatch()
            }
        }

        companion object {
            internal lateinit var eventReceiver: EventReceiver private set
            private var initialed = false
            fun setupEventReceiver(context: Context) {
                if (!initialed) {
                    eventReceiver = EventReceiver()
                    context.applicationContext.registerReceiverCompat(
                        eventReceiver,
                        IntentFilter(MusicServiceMsgConst.MEDIA_STORE_CHANGED),
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                    initialed = true
                }
            }
        }
    }

    companion object {
        private const val TAG = "MediaStoreTracker"
    }
}

