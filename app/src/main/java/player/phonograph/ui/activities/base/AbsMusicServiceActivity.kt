/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities.base

import lib.phonograph.activity.ToolbarActivity
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicPlayerRemote.ServiceToken
import player.phonograph.service.queue.CurrentQueueState
import player.phonograph.service.queue.QueueManager
import player.phonograph.settings.BROADCAST_CURRENT_PLAYER_STATE
import player.phonograph.settings.CLASSIC_NOTIFICATION
import player.phonograph.settings.COLORED_NOTIFICATION
import player.phonograph.settings.GAPLESS_PLAYBACK
import player.phonograph.settings.SettingFlowStore
import player.phonograph.util.debug
import player.phonograph.util.permissions.NonGrantedPermission
import player.phonograph.util.permissions.checkStorageReadPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import android.content.ComponentName
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.lang.System.currentTimeMillis

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMusicServiceActivity : ToolbarActivity(), MusicServiceEventListener {

    protected val queueManager: QueueManager by inject()

    private var serviceToken: ServiceToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val permission = checkStorageReadPermission(this@AbsMusicServiceActivity)
            if (permission is NonGrantedPermission) {
                withResumed {
                    notifyPermissionDeniedUser(listOf(permission)) {
                        requestPermissionImpl(arrayOf(permission.permissionId)) { result ->
                            if (result.entries.first().value) {
                                GlobalContext.get().get<MediaStoreTracker>().notifyAllListeners()
                            }
                        }
                    }
                }
            }
        }

        debug {
            Log.v(
                "Metrics",
                "${currentTimeMillis().mod(10000000)} AbsMusicServiceActivity start Music Service"
            )
        }
        serviceToken =
            MusicPlayerRemote.bindToService(
                this,
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) {
                        this@AbsMusicServiceActivity.onServiceConnected()
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        this@AbsMusicServiceActivity.onServiceDisconnected()
                    }
                }
            )
        debug {
            Log.v(
                "Metrics",
                "${currentTimeMillis().mod(10000000)} AbsMusicServiceActivity Music Service is started"
            )
        }
        volumeControlStream = AudioManager.STREAM_MUSIC
        lifecycle.addObserver(LifeCycleObserver())
        observeSetting()
    }
    private fun observeSetting() {
        fun observe(block: suspend CoroutineScope.() -> Unit) {
            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED, block)
            }
        }
        val store = SettingFlowStore(this)
        observe {
            store.gaplessPlayback.distinctUntilChanged()
                .collect { MusicPlayerRemote.musicService?.updateSetting(GAPLESS_PLAYBACK, it) }
        }
        observe {
            store.coloredNotification.distinctUntilChanged()
                .collect { MusicPlayerRemote.musicService?.updateSetting(COLORED_NOTIFICATION, it) }
        }
        observe {
            store.broadcastCurrentPlayerState.distinctUntilChanged()
                .collect { MusicPlayerRemote.musicService?.updateSetting(BROADCAST_CURRENT_PLAYER_STATE, it) }
        }
        observe {
            store.classicNotification.distinctUntilChanged()
                .collect { MusicPlayerRemote.musicService?.updateSetting(CLASSIC_NOTIFICATION, it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicPlayerRemote.unbindFromService(serviceToken)
    }

    //
    // Register
    //

    private val mMusicServiceEventListeners: MutableList<MusicServiceEventListener> = ArrayList()

    fun addMusicServiceEventListener(listener: MusicServiceEventListener) {
        mMusicServiceEventListeners.add(listener)
    }

    fun removeMusicServiceEventListener(listener: MusicServiceEventListener) {
        mMusicServiceEventListeners.remove(listener)
    }

    //
    // CurrentQueueState
    //

    inner class LifeCycleObserver : DefaultLifecycleObserver {

        private var registered = false

        private val shouldUnregister get() = mMusicServiceEventListeners.isEmpty()

        override fun onCreate(owner: LifecycleOwner) {
            if (!registered) {
                registered = true
                CurrentQueueState.init(queueManager)
                CurrentQueueState.register(queueManager)
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            if (shouldUnregister) {
                CurrentQueueState.unregister(queueManager)
                registered = false
            }
        }
    }

    //
    // MusicServiceEventListener Callbacks
    //

    override fun onServiceConnected() {
        for (listener in mMusicServiceEventListeners) {
            listener.onServiceConnected()
        }
    }

    override fun onServiceDisconnected() {
        for (listener in mMusicServiceEventListeners) {
            listener.onServiceDisconnected()
        }
    }

}

interface MusicServiceEventListener {
    fun onServiceConnected()
    fun onServiceDisconnected()
}