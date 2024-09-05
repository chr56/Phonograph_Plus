/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.service.player

import player.phonograph.service.MusicService
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ContentDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.jpeg.JpegExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.ogg.OggExtractor
import androidx.media3.extractor.png.PngExtractor
import androidx.media3.extractor.ts.Ac3Extractor
import androidx.media3.extractor.ts.Ac4Extractor
import androidx.media3.extractor.wav.WavExtractor
import androidx.media3.extractor.webp.WebpExtractor
import android.content.Context
import android.os.Handler


class ExoAudioPlayer(
    context: MusicService, val handler: Handler, override var gaplessPlayback: Boolean,
) : Playback, Player.Listener {

    private val exoPlayer: ExoPlayer = createExoPlayer(context)

    override var callbacks: Playback.PlaybackCallbacks? = null

    override var isInitialized: Boolean = false
        private set

    override var currentDataSource: String = ""
        private set

    constructor(
        context: MusicService,
        handler: Handler,
        gaplessPlayback: Boolean,
        callbacks: Playback.PlaybackCallbacks,
    ) : this(context, handler, gaplessPlayback) {
        this.callbacks = callbacks
    }

    override fun setDataSource(path: String): Boolean {
        isInitialized = false
        handler.post {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.addMediaItem(MediaItem.fromUri(path))
            exoPlayer.prepare()
            currentDataSource = path
            isInitialized = true
        }
        return true
    }

    override fun setNextDataSource(path: String?) {
        // todo
    }


    override fun play(): Boolean {
        if (isInitialized) {
            exoPlayer.play()
            return true
        } else {
            return false
        }
    }

    override fun stop() {
        exoPlayer.stop()
    }

    override fun release() {
        exoPlayer.release()
    }

    override fun pause(): Boolean {
        exoPlayer.pause()
        return true
    }

    override val isPlaying: Boolean get() = _isPlaying

    override fun duration(): Int = _duration.toInt()
    override fun position(): Int = _contentPosition.toInt()

    override fun seek(whereto: Int): Int {
        exoPlayer.seekTo(whereto.toLong())
        return whereto
    }

    override fun setVolume(vol: Float): Boolean {
        exoPlayer.volume = vol
        return true
    }

    override val audioSessionId: Int
        @OptIn(UnstableApi::class)
        get() = exoPlayer.audioSessionId

    @OptIn(UnstableApi::class)
    override fun setAudioSessionId(sessionId: Int): Boolean {
        exoPlayer.audioSessionId = sessionId
        return true
    }

    private var _speed: Float = 1.0f

    override var speed: Float
        get() = _speed
        set(value) = exoPlayer.setPlaybackSpeed(value)


    private var _isPlaying: Boolean = false
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying = isPlaying
        progressLoop()
    }

    private var _duration: Long = -1L
    private var _contentPosition: Long = -1L
    private fun accessProgress(): Boolean {
        _duration = exoPlayer.duration
        _contentPosition = exoPlayer.contentPosition
        return _isPlaying
    }

    fun progressLoop() {
        handler.postDelayed({
            if (accessProgress()) progressLoop()
        }, 720)
    }

    @OptIn(UnstableApi::class)
    private fun createExoPlayer(context: Context): ExoPlayer {

        val audioOnlyRenderersFactory = RenderersFactory {
                handler: Handler,
                _: VideoRendererEventListener,
                audioListener: AudioRendererEventListener,
                _: TextOutput,
                _: MetadataOutput,
            ->
            arrayOf<Renderer>(
                MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, handler, audioListener)
            )
        }
        val mediaSourceFactory = createMediaSourceFactory(context)
        val player =
            ExoPlayer.Builder(context, audioOnlyRenderersFactory, mediaSourceFactory)
                .setHandleAudioBecomingNoisy(true)
                .setLooper(handler.looper)
                .build()
        player.addListener(this)
        return player
    }

    @OptIn(UnstableApi::class)
    private fun createMediaSourceFactory(context: Context): MediaSource.Factory {
        val extractorsFactory = ExtractorsFactory {
            val constantBitrateSeeking = false
            arrayOf(
                /*
                  Audio Formats
                 */
                Ac3Extractor(),
                Ac4Extractor(),
                OggExtractor(),
                FlacExtractor(),
                Mp3Extractor(if (constantBitrateSeeking) Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING else 0),
                AmrExtractor(if (constantBitrateSeeking) AmrExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING else 0),
                // AdtsExtractor(if (constantBitrateSeeking) AdtsExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING else 0),
                WavExtractor(),
                /*
                  Images Formats
                 */
                PngExtractor(),
                JpegExtractor(),
                WebpExtractor(),
            )
        }
        return ProgressiveMediaSource.Factory({ ContentDataSource(context) }, extractorsFactory)
    }
}