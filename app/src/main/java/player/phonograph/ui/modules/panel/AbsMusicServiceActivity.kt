/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.modules.panel

import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.inject
import player.phonograph.R
import player.phonograph.foundation.concurrent.runOnMainHandler
import player.phonograph.foundation.permission.StoragePermissionChecker
import player.phonograph.mechanism.event.MediaStoreObservation
import player.phonograph.model.service.MusicServiceConnection
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicPlayerRemote.ServiceToken
import player.phonograph.service.queue.QueueManager
import player.phonograph.ui.basis.ThemeActivity
import player.phonograph.ui.navigateToAppDetailSetting
import player.phonograph.ui.resource.Texts
import player.phonograph.ui.theme.ThemeSettingsDelegate.accentColor
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import kotlinx.coroutines.launch

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMusicServiceActivity : ThemeActivity(), MusicServiceEventListener {

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
            notifyPermissionDeniedUser(listOf(StoragePermissionChecker.necessaryStorageReadPermission))
        }
    }

    private suspend fun notifyPermissionDeniedUser(missingPermissions: List<String>) {
        if (missingPermissions.isEmpty()) return

        val message = StringBuffer(getString(R.string.err_permissions_denied)).append('\n')
        var requireGotoSetting = false
        for (permission in missingPermissions) {
            message
                .append(Texts.permissionName(this, permission)).append('\n')
                .append(Texts.permissionDescription(this, permission)).append('\n')
            if (shouldShowRequestPermissionRationale(permission)) requireGotoSetting = true
        }
        withResumed {
            val snackBar = Snackbar.make(snackBarContainer, message, Snackbar.LENGTH_INDEFINITE)
            snackBar.anchorView = snackBarAnchor
            if (requireGotoSetting) {
                snackBar.setAction(R.string.action_settings) { navigateToAppDetailSetting(this) }
            } else {
                snackBar.setAction(R.string.action_grant) { navigateToAppDetailSetting(this) }
            }
            snackBar.setActionTextColor(accentColor()).setTextMaxLines(Int.MAX_VALUE)
            runOnMainHandler { snackBar.show() }
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

    //
    // SnackBar holder
    //
    protected open val snackBarContainer: View get() = window.decorView
    protected open val snackBarAnchor: View? get() = null

}
