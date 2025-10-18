/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.modules.panel

import org.koin.android.ext.android.inject
import player.phonograph.mechanism.event.MediaStoreObservation
import player.phonograph.model.service.MusicServiceConnection
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicPlayerRemote.ServiceToken
import player.phonograph.service.queue.QueueManager
import player.phonograph.ui.basis.ToolbarActivity
import player.phonograph.util.permissions.StoragePermissionChecker
import player.phonograph.util.permissions.navigateToAppDetailSetting
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import kotlinx.coroutines.launch

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMusicServiceActivity : ToolbarActivity(), MusicServiceEventListener {

    protected val queueManager: QueueManager by inject()
    protected val queueViewModel: QueueViewModel by viewModels()

    private val contentLifecycleObserver: MediaStoreObservation.LifecycleObserver =
        MediaStoreObservation.LifecycleObserver()

    private var serviceToken: ServiceToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        queueViewModel.refresh(queueManager)
        queueViewModel.register(queueManager)

        lifecycleScope.launch {
            connectToService()
        }
        lifecycleScope.launch {
            checkStorageReadPermission()
        }
        lifecycle.addObserver(contentLifecycleObserver)
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onDestroy() {
        super.onDestroy()
        queueViewModel.unregister(queueManager)
        disconnectFromService()
    }

    suspend fun connectToService() {
        serviceToken =
            MusicPlayerRemote.bindToService(
                this@AbsMusicServiceActivity,
                object : MusicServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) {
                        this@AbsMusicServiceActivity.onServiceConnected()
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        this@AbsMusicServiceActivity.onServiceDisconnected()
                    }

                    override fun onServiceDetached() {
                        this@AbsMusicServiceActivity.onServiceDisconnected()
                    }
                }
            )
    }

    fun disconnectFromService() {
        MusicPlayerRemote.unbindFromService(serviceToken)
    }

    private suspend fun checkStorageReadPermission() {
        val result = StoragePermissionChecker.hasStorageReadPermission(this)
        if (!result) {
            withResumed {
                notifyPermissionDeniedUser(listOf(StoragePermissionChecker.necessaryStorageReadPermission)) {
                    navigateToAppDetailSetting(this)
                }
            }
        }
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

