/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities.base

import lib.phonograph.activity.ToolbarActivity
import player.phonograph.App
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicPlayerRemote.ServiceToken
import player.phonograph.service.queue.CurrentQueueState
import player.phonograph.util.debug
import player.phonograph.util.permissions.NonGrantedPermission
import player.phonograph.util.permissions.Permission
import player.phonograph.util.permissions.checkStorageReadPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.content.ComponentName
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import java.lang.System.currentTimeMillis

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMusicServiceActivity : ToolbarActivity(), MusicServiceEventListener {

    private var serviceToken: ServiceToken? = null

    private lateinit var permission: Permission

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        permission = checkStorageReadPermission(this)

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
    }

    override fun onStart() {
        super.onStart()
        if (permission is NonGrantedPermission) {
            notifyPermissionDeniedUser(listOf(permission)) {
                requestPermissionImpl(arrayOf(permission.permissionId)) { result ->
                    if (result.entries.first().value) MediaStoreTracker.notifyAllListeners()
                }
            }
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
                CurrentQueueState.init(App.instance.queueManager)
                CurrentQueueState.register(App.instance.queueManager)
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            if (shouldUnregister) {
                CurrentQueueState.unregister(App.instance.queueManager)
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