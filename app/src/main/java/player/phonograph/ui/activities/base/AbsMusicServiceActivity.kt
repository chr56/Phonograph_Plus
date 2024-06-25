/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities.base

import lib.phonograph.activity.ToolbarActivity
import org.koin.android.ext.android.inject
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicPlayerRemote.ServiceToken
import player.phonograph.service.queue.CurrentQueueState
import player.phonograph.service.queue.QueueManager
import player.phonograph.util.permissions.StoragePermissionChecker
import player.phonograph.util.permissions.navigateToAppDetailSetting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import android.content.ComponentName
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import kotlinx.coroutines.launch

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMusicServiceActivity : ToolbarActivity(), MusicServiceEventListener {

    protected val queueManager: QueueManager by inject()

    private var serviceToken: ServiceToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            serviceToken =
                MusicPlayerRemote.bindToService(
                    this@AbsMusicServiceActivity,
                    object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, service: IBinder) {
                            this@AbsMusicServiceActivity.onServiceConnected()
                        }

                        override fun onServiceDisconnected(name: ComponentName) {
                            this@AbsMusicServiceActivity.onServiceDisconnected()
                        }
                    }
                )
            volumeControlStream = AudioManager.STREAM_MUSIC
            val result = StoragePermissionChecker.hasStorageReadPermission(this@AbsMusicServiceActivity)
            if (!result) {
                withResumed {
                    notifyPermissionDeniedUser(listOf(StoragePermissionChecker.necessaryStorageReadPermission)) {
                        navigateToAppDetailSetting(this@AbsMusicServiceActivity)
                    }
                }
            }
        }
        lifecycle.addObserver(LifeCycleObserver())
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