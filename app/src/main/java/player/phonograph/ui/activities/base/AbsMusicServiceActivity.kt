/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities.base

import android.Manifest
import android.content.*
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import java.lang.ref.WeakReference
import lib.phonograph.activity.ToolbarActivity
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.interfaces.MusicServiceEventListener
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicPlayerRemote.ServiceToken
import player.phonograph.service.MusicService

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMusicServiceActivity : ToolbarActivity(), MusicServiceEventListener {

    private var serviceToken: ServiceToken? = null

    private var musicStateReceiver: MusicStateReceiver? = null
    private var receiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (DEBUG) Log.v(
            "Metrics",
            "${System.currentTimeMillis().mod(10000000)} AbsMusicServiceActivity start Music Service"
        )
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
        if (DEBUG) Log.v(
            "Metrics",
            "${System.currentTimeMillis().mod(10000000)} AbsMusicServiceActivity Music Service is started"
        )
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicPlayerRemote.unbindFromService(serviceToken)
        if (receiverRegistered) {
            unregisterReceiver(musicStateReceiver)
            receiverRegistered = false
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
        if (!receiverRegistered) {
            musicStateReceiver = MusicStateReceiver(this)
            registerReceiver(
                musicStateReceiver,
                IntentFilter().apply {
                    addAction(MusicService.PLAY_STATE_CHANGED)
                    addAction(MusicService.SHUFFLE_MODE_CHANGED)
                    addAction(MusicService.REPEAT_MODE_CHANGED)
                    addAction(MusicService.META_CHANGED)
                    addAction(MusicService.QUEUE_CHANGED)
                    addAction(MusicService.MEDIA_STORE_CHANGED)
                }
            )
            receiverRegistered = true
        }
        for (listener in mMusicServiceEventListeners) {
            listener.onServiceConnected()
        }
    }

    override fun onServiceDisconnected() {
        if (receiverRegistered) {
            unregisterReceiver(musicStateReceiver)
            receiverRegistered = false
        }
        for (listener in mMusicServiceEventListeners) {
            listener.onServiceDisconnected()
        }
    }

    override fun onPlayingMetaChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onPlayingMetaChanged()
        }
    }

    override fun onQueueChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onQueueChanged()
        }
    }

    override fun onPlayStateChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onPlayStateChanged()
        }
    }

    override fun onMediaStoreChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onMediaStoreChanged()
        }
    }

    override fun onRepeatModeChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onRepeatModeChanged()
        }
    }

    override fun onShuffleModeChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onShuffleModeChanged()
        }
    }

    //
    // Receiver
    //

    private class MusicStateReceiver(activity: AbsMusicServiceActivity) : BroadcastReceiver() {
        private val reference: WeakReference<AbsMusicServiceActivity> = WeakReference(activity)
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            reference.get()?.also { activity ->
                when (action) {
                    MusicService.META_CHANGED -> activity.onPlayingMetaChanged()
                    MusicService.QUEUE_CHANGED -> activity.onQueueChanged()
                    MusicService.PLAY_STATE_CHANGED -> activity.onPlayStateChanged()
                    MusicService.REPEAT_MODE_CHANGED -> activity.onRepeatModeChanged()
                    MusicService.SHUFFLE_MODE_CHANGED -> activity.onShuffleModeChanged()
                    MusicService.MEDIA_STORE_CHANGED -> activity.onMediaStoreChanged()
                }
            }
        }
    }

    override fun missingPermissionCallback() {
        super.missingPermissionCallback()
        sendBroadcast(
            Intent(MusicService.MEDIA_STORE_CHANGED).apply {
                putExtra("from_permissions_changed", true) // just in case we need to know this at some point
            }
        )
    }

    override fun getPermissionsToRequest(): Array<String>? {
        return arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override val permissionDeniedMessage: String get() = getString(
        R.string.permission_external_storage_denied
    )
}
