package player.phonograph.helper

import android.os.Handler
import android.os.Looper
import android.os.Message
import player.phonograph.service.MusicPlayerRemote

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class MusicProgressViewUpdateHelper : Handler {
    private var callback: Callback?
    private var intervalPlaying: Int
    private var intervalPaused: Int

    fun start() {
        queueNextRefresh(1)
    }
    fun stop() {
        removeMessages(CMD_REFRESH_PROGRESS_VIEWS)
    }
    fun destroy() {
        stop()
        if (looper != Looper.getMainLooper()) looper.quit()
        callback = null
    }

    constructor(callback: Callback, looper: Looper = Looper.getMainLooper()) : super(looper) {
        this.callback = callback
        intervalPlaying = UPDATE_INTERVAL_PLAYING
        intervalPaused = UPDATE_INTERVAL_PAUSED
    }
    constructor(callback: Callback, intervalPlaying: Int, intervalPaused: Int, looper: Looper = Looper.getMainLooper()) : super(looper) {
        this.callback = callback
        this.intervalPlaying = intervalPlaying
        this.intervalPaused = intervalPaused
    }

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        if (msg.what == CMD_REFRESH_PROGRESS_VIEWS) {
            queueNextRefresh(refreshProgressViews().toLong())
        }
    }

    @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
    private fun refreshProgressViews(): Int {
        val progressMillis = MusicPlayerRemote.songProgressMillis
        val totalMillis = MusicPlayerRemote.songDurationMillis
        callback?.onUpdateProgressViews(progressMillis, totalMillis)
        if (!MusicPlayerRemote.isPlaying) {
            return intervalPaused
        }
        val remainingMillis = intervalPlaying - progressMillis % intervalPlaying
        return Math.max(MIN_INTERVAL, remainingMillis)
    }

    private fun queueNextRefresh(delay: Long) {
        val message = obtainMessage(CMD_REFRESH_PROGRESS_VIEWS)
        removeMessages(CMD_REFRESH_PROGRESS_VIEWS)
        sendMessageDelayed(message, delay)
    }

    interface Callback {
        fun onUpdateProgressViews(progress: Int, total: Int)
    }

    companion object {
        private const val CMD_REFRESH_PROGRESS_VIEWS = 1
        private const val MIN_INTERVAL = 20
        private const val UPDATE_INTERVAL_PLAYING = 1000
        private const val UPDATE_INTERVAL_PAUSED = 500
    }
}
