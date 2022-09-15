/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities.base

import android.Manifest
import android.content.*
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import java.lang.ref.WeakReference
import lib.phonograph.activity.ToolbarActivity
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.MEDIA_STORE_CHANGED
import player.phonograph.MusicServiceMsgConst
import player.phonograph.R
import player.phonograph.interfaces.MusicServiceEventListener
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicPlayerRemote.ServiceToken

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
                    addAction(MusicServiceMsgConst.PLAY_STATE_CHANGED)
                    addAction(MusicServiceMsgConst.SHUFFLE_MODE_CHANGED)
                    addAction(MusicServiceMsgConst.REPEAT_MODE_CHANGED)
                    addAction(MusicServiceMsgConst.META_CHANGED)
                    addAction(MusicServiceMsgConst.QUEUE_CHANGED)
                    addAction(MEDIA_STORE_CHANGED)
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
                    MusicServiceMsgConst.META_CHANGED -> activity.onPlayingMetaChanged()
                    MusicServiceMsgConst.QUEUE_CHANGED -> activity.onQueueChanged()
                    MusicServiceMsgConst.PLAY_STATE_CHANGED -> activity.onPlayStateChanged()
                    MusicServiceMsgConst.REPEAT_MODE_CHANGED -> activity.onRepeatModeChanged()
                    MusicServiceMsgConst.SHUFFLE_MODE_CHANGED -> activity.onShuffleModeChanged()
                    MEDIA_STORE_CHANGED -> activity.onMediaStoreChanged()
                }
            }
        }
    }

    override fun missingPermissionCallback() {
        super.missingPermissionCallback()
        sendBroadcast(
            Intent(MEDIA_STORE_CHANGED).apply {
                putExtra("from_permissions_changed", true) // just in case we need to know this at some point
            }
        )
    }

    override fun getPermissionsToRequest(): Array<String>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
    }

    override val permissionDeniedMessage: String
        get() = getString(
            R.string.permission_external_storage_denied
        )
}
