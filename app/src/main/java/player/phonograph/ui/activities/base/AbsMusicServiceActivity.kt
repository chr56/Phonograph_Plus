/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities.base

import lib.phonograph.activity.ToolbarActivity
import player.phonograph.MusicServiceMsgConst
import player.phonograph.misc.MusicServiceEventListener
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicPlayerRemote.ServiceToken
import player.phonograph.util.Util.debug
import player.phonograph.util.permissions.NonGrantedPermission
import player.phonograph.util.permissions.Permission
import player.phonograph.util.permissions.checkStorageReadPermission
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import java.lang.System.currentTimeMillis
import java.lang.ref.WeakReference

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMusicServiceActivity : ToolbarActivity(), MusicServiceEventListener {

    private var serviceToken: ServiceToken? = null

    private var musicStateReceiver: MusicStateReceiver? = null
    private var receiverRegistered = false

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
    }

    override fun onStart() {
        super.onStart()
        if (permission is NonGrantedPermission) {
            notifyPermissionDeniedUser(listOf(permission)) {
                requestPermissionImpl(arrayOf(permission.permissionId)) { result ->
                    if (result.entries.first().value)
                        sendBroadcast(Intent(MusicServiceMsgConst.MEDIA_STORE_CHANGED))
                }
            }
        }
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
                    addAction(MusicServiceMsgConst.MEDIA_STORE_CHANGED)
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
                    MusicServiceMsgConst.META_CHANGED         -> activity.onPlayingMetaChanged()
                    MusicServiceMsgConst.QUEUE_CHANGED        -> activity.onQueueChanged()
                    MusicServiceMsgConst.PLAY_STATE_CHANGED   -> activity.onPlayStateChanged()
                    MusicServiceMsgConst.REPEAT_MODE_CHANGED  -> activity.onRepeatModeChanged()
                    MusicServiceMsgConst.SHUFFLE_MODE_CHANGED -> activity.onShuffleModeChanged()
                    MusicServiceMsgConst.MEDIA_STORE_CHANGED  -> activity.onMediaStoreChanged()
                }
            }
        }
    }

}
