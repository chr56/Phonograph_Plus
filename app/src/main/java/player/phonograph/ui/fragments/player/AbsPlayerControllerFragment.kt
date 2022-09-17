/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.player

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import mt.util.color.isColorLight
import mt.util.color.primaryTextColor
import mt.util.color.secondaryDisabledTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.helper.MusicProgressViewUpdateHelper
import lib.phonograph.misc.SimpleOnSeekbarChangeListener
import player.phonograph.model.getReadableDurationString
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.views.PlayPauseDrawable

abstract class AbsPlayerControllerFragment : AbsMusicServiceFragment(), MusicProgressViewUpdateHelper.Callback {

    protected lateinit var playPauseDrawable: PlayPauseDrawable

    protected lateinit var prevButton: ImageButton
    protected lateinit var nextButton: ImageButton

    protected lateinit var repeatButton: ImageButton
    protected lateinit var shuffleButton: ImageButton

    protected lateinit var progressSlider: SeekBar
    protected lateinit var songTotalTime: TextView
    protected lateinit var songCurrentProgress: TextView

    protected var lastPlaybackControlsColor = 0
    private var lastDisabledPlaybackControlsColor = 0

    private var _progressViewUpdateHelper: MusicProgressViewUpdateHelper? = null
    private val progressViewUpdateHelper: MusicProgressViewUpdateHelper get() = _progressViewUpdateHelper!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _progressViewUpdateHelper = MusicProgressViewUpdateHelper(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        _progressViewUpdateHelper = null
    }

    protected abstract fun bindView(inflater: LayoutInflater): View
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return bindView(inflater)
    }

    protected abstract fun unbindView()
    override fun onDestroyView() {
        super.onDestroyView()
        unbindView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpPlayPauseButton()
        setUpPrevNext()
        setUpShuffleButton()
        setUpRepeatButton()

        setUpProgressSlider()

        updateProgressTextColor()
    }

    abstract fun setUpPlayPauseButton()

    private fun setUpPrevNext() {
        updatePrevNextColor()
        nextButton.setOnClickListener { MusicPlayerRemote.playNextSong() }
        prevButton.setOnClickListener { MusicPlayerRemote.back() }
    }

    private fun setUpShuffleButton() {
        shuffleButton.setOnClickListener { MusicPlayerRemote.toggleShuffleMode() }
    }

    private fun setUpRepeatButton() {
        repeatButton.setOnClickListener { MusicPlayerRemote.cycleRepeatMode() }
    }

    private fun setUpProgressSlider() {
        val color = requireContext().primaryTextColor(true)
        val colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
        progressSlider.thumb.mutate().colorFilter = colorFilter
        progressSlider.progressDrawable.mutate().colorFilter = colorFilter
        progressSlider.setOnSeekBarChangeListener(object : SimpleOnSeekbarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    MusicPlayerRemote.seekTo(progress)
                    onUpdateProgressViews(MusicPlayerRemote.songProgressMillis, MusicPlayerRemote.songDurationMillis)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
    }

    // Listeners

    override fun onServiceConnected() {
        updatePlayPauseDrawableState(false)
        updateRepeatState()
        updateShuffleState()
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState(true)
    }

    override fun onRepeatModeChanged() {
        updateRepeatState()
    }

    override fun onShuffleModeChanged() {
        updateShuffleState()
    }

    private fun calculateColor(backgroundColor: Int) {
        val context = requireContext()
        val darkmode = !isColorLight(backgroundColor)

        lastPlaybackControlsColor = context.secondaryTextColor(darkmode)
        lastDisabledPlaybackControlsColor = context.secondaryDisabledTextColor(darkmode)
    }

    fun modifyColor(backgroundColor: Int) {
        calculateColor(backgroundColor)

        updateRepeatState()
        updateShuffleState()
        updatePrevNextColor()
        updatePlayPauseColor()
        updateProgressTextColor()
    }

    // Update state
    protected abstract fun updatePlayPauseDrawableState(animate: Boolean)
    protected open fun updatePlayPauseColor() {}
    private fun updatePrevNextColor() {
        nextButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        prevButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        progressSlider.max = total
        progressSlider.progress = progress
        songTotalTime.text = getReadableDurationString(total.toLong())
        songCurrentProgress.text = getReadableDurationString(progress.toLong())
    }

    private fun updateProgressTextColor() {
        val color = requireContext().primaryTextColor(true)
        songTotalTime.setTextColor(color)
        songCurrentProgress.setTextColor(color)
    }

    private fun updateRepeatState() {
        when (MusicPlayerRemote.repeatMode) {
            RepeatMode.NONE -> {
                repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp)
                repeatButton.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
            }
            RepeatMode.REPEAT_QUEUE -> {
                repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp)
                repeatButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
            }
            RepeatMode.REPEAT_SINGLE_SONG -> {
                repeatButton.setImageResource(R.drawable.ic_repeat_one_white_24dp)
                repeatButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
            }
        }
    }

    private fun updateShuffleState() {
        when (MusicPlayerRemote.shuffleMode) {
            ShuffleMode.SHUFFLE ->
                shuffleButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
            ShuffleMode.NONE ->
                shuffleButton.setColorFilter(lastDisabledPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
        }
    }

    abstract fun show()
    abstract fun hide()
}
