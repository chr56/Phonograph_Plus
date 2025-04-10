/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.util.component

import player.phonograph.model.ui.ProgressUpdateCallback
import player.phonograph.service.MusicPlayerRemote
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.os.Handler
import android.os.Looper
import android.os.Message
import kotlin.math.max


class MusicProgressUpdateDelegate(
    var callback: ProgressUpdateCallback?,
    val intervalPlaying: Int = UPDATE_INTERVAL_PLAYING,
    val intervalPaused: Int = UPDATE_INTERVAL_PAUSED,
) : DefaultLifecycleObserver, ProgressUpdateCallback {

    private var _updateHelper: MusicProgressUpdateHandler? = null
    private val updateHelper get() = _updateHelper!!

    override fun onCreate(owner: LifecycleOwner) {
        _updateHelper = MusicProgressUpdateHandler(this, intervalPlaying, intervalPaused)
    }

    override fun onResume(owner: LifecycleOwner) {
        updateHelper.start()
    }

    override fun onPause(owner: LifecycleOwner) {
        updateHelper.stop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        _updateHelper?.destroy()
        _updateHelper = null
        callback = null
    }

    override fun onUpdateProgress(progress: Int, total: Int) {
        callback?.onUpdateProgress(progress, total)
    }
}


/**
 * @author Karim Abou Zeid (kabouzeid)
 */
private class MusicProgressUpdateHandler(
    private var callback: ProgressUpdateCallback?,
    private var intervalPlaying: Int,
    private var intervalPaused: Int,
    looper: Looper = Looper.getMainLooper(),
) : Handler(looper) {

    fun start() {
        queueNextRefresh(1)
    }

    fun stop() {
        removeMessages(CMD_UPDATE)
    }

    fun destroy() {
        stop()
        if (looper != Looper.getMainLooper()) looper.quit()
        callback = null
    }

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        if (msg.what == CMD_UPDATE) {
            queueNextRefresh(refreshProgressViews().toLong())
        }
    }

    private fun refreshProgressViews(): Int {
        val progressMillis = MusicPlayerRemote.songProgressMillis
        val totalMillis = MusicPlayerRemote.songDurationMillis
        callback?.onUpdateProgress(progressMillis, totalMillis)
        if (!MusicPlayerRemote.isPlaying) {
            return intervalPaused
        }
        val remainingMillis = intervalPlaying - progressMillis % intervalPlaying
        return max(MIN_INTERVAL, remainingMillis)
    }

    private fun queueNextRefresh(delay: Long) {
        val message = obtainMessage(CMD_UPDATE)
        removeMessages(CMD_UPDATE)
        sendMessageDelayed(message, delay)
    }

}

private const val CMD_UPDATE = 1
private const val MIN_INTERVAL = 20

private const val UPDATE_INTERVAL_PLAYING = 1000
private const val UPDATE_INTERVAL_PAUSED = 500