/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities.base

import android.Manifest
import android.content.*
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import lib.phonograph.activity.ToolbarActivity
import player.phonograph.R
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicPlayerRemote.ServiceToken
import player.phonograph.interfaces.MusicServiceEventListener
import player.phonograph.service.MusicService
import java.lang.ref.WeakReference

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class AbsMusicServiceActivity : ToolbarActivity(), MusicServiceEventListener {

    private var serviceToken: ServiceToken? = null

    private val mMusicServiceEventListeners: MutableList<MusicServiceEventListener> = ArrayList()
    private var musicStateReceiver: MusicStateReceiver? = null
    private var receiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        volumeControlStream = AudioManager.STREAM_MUSIC
        permissionDeniedMessage = getString(R.string.permission_external_storage_denied)
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

    fun addMusicServiceEventListener(listener: MusicServiceEventListener?) {
        if (listener != null) mMusicServiceEventListeners.add(listener)
    }

    fun removeMusicServiceEventListener(listener: MusicServiceEventListener?) {
        if (listener != null) mMusicServiceEventListeners.remove(listener)
    }

    //
    // MusicServiceEventListener Callbacks
    //

    override fun onServiceConnected() {
        if (!receiverRegistered) {
            musicStateReceiver = MusicStateReceiver(this)
            val filter = IntentFilter()
            filter.addAction(MusicService.PLAY_STATE_CHANGED)
            filter.addAction(MusicService.SHUFFLE_MODE_CHANGED)
            filter.addAction(MusicService.REPEAT_MODE_CHANGED)
            filter.addAction(MusicService.META_CHANGED)
            filter.addAction(MusicService.QUEUE_CHANGED)
            filter.addAction(MusicService.MEDIA_STORE_CHANGED)
            registerReceiver(musicStateReceiver, filter)
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
            val activity = reference.get()
            if (activity != null) {
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

    //
    // Implement Permissions
    //

    override fun onHasPermissionsChanged(hasPermissions: Boolean) {
        super.onHasPermissionsChanged(hasPermissions)
        val intent = Intent(MusicService.MEDIA_STORE_CHANGED)
        intent.putExtra("from_permissions_changed", true) // just in case we need to know this at some point
        sendBroadcast(intent)
    }

    override fun getPermissionsToRequest(): Array<String>? {
        return arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
