/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.player

import mt.util.color.isColorLight
import mt.util.color.primaryTextColor
import mt.util.color.secondaryDisabledTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.misc.MusicProgressViewUpdateHelperDelegate
import player.phonograph.misc.SimpleOnSeekbarChangeListener
import player.phonograph.model.getReadableDurationString
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.ui.views.PlayPauseDrawable
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.content.Context
import android.graphics.PorterDuff.Mode.SRC_IN
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class AbsPlayerControllerFragment : AbsMusicServiceFragment() {

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

    private val progressViewUpdateHelperDelegate =
        MusicProgressViewUpdateHelperDelegate(::updateProgressViews)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(progressViewUpdateHelperDelegate)
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
        val colorFilter = createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
        progressSlider.thumb.mutate().colorFilter = colorFilter
        progressSlider.progressDrawable.mutate().colorFilter = colorFilter
        progressSlider.setOnSeekBarChangeListener(object : SimpleOnSeekbarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    MusicPlayerRemote.seekTo(progress)
                    updateProgressViews(
                        MusicPlayerRemote.songProgressMillis,
                        MusicPlayerRemote.songDurationMillis
                    )
                }
            }
        })
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

    private fun calculateColor(context: Context, backgroundColor: Int) {
        val darkmode = !isColorLight(backgroundColor)
        lastPlaybackControlsColor = context.secondaryTextColor(darkmode)
        lastDisabledPlaybackControlsColor = context.secondaryDisabledTextColor(darkmode)
    }

    fun modifyColor(backgroundColor: Int) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                require(context != null) { "ControllerFragment is not available now!" }
                calculateColor(requireContext(), backgroundColor)
                updateAll()
            }
        }
    }

    private suspend fun updateAll() = withContext(Dispatchers.Main) {
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
        nextButton.setColorFilter(lastPlaybackControlsColor, SRC_IN)
        prevButton.setColorFilter(lastPlaybackControlsColor, SRC_IN)
    }

    private fun updateProgressTextColor() {
        val color = requireContext().primaryTextColor(true)
        songTotalTime.setTextColor(color)
        songCurrentProgress.setTextColor(color)
    }

    private fun updateProgressViews(progress: Int, total: Int) {
        progressSlider.max = total
        progressSlider.progress = progress
        songTotalTime.text = getReadableDurationString(total.toLong())
        songCurrentProgress.text = getReadableDurationString(progress.toLong())
    }

    private fun updateRepeatState() = when (MusicPlayerRemote.repeatMode) {
        RepeatMode.NONE               -> {
            repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp)
            repeatButton.setColorFilter(lastDisabledPlaybackControlsColor, SRC_IN)
        }
        RepeatMode.REPEAT_QUEUE       -> {
            repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp)
            repeatButton.setColorFilter(lastPlaybackControlsColor, SRC_IN)
        }
        RepeatMode.REPEAT_SINGLE_SONG -> {
            repeatButton.setImageResource(R.drawable.ic_repeat_one_white_24dp)
            repeatButton.setColorFilter(lastPlaybackControlsColor, SRC_IN)
        }
    }

    private fun updateShuffleState() = shuffleButton.setColorFilter(
        when (MusicPlayerRemote.shuffleMode) {
            ShuffleMode.SHUFFLE -> lastPlaybackControlsColor
            ShuffleMode.NONE    -> lastDisabledPlaybackControlsColor
        },
        SRC_IN
    )

    abstract fun show()
    abstract fun hide()
}
