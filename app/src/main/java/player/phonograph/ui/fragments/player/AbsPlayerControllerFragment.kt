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
import player.phonograph.service.player.PlayerController
import player.phonograph.service.player.currentState
import player.phonograph.service.queue.CurrentQueueState
import player.phonograph.service.queue.RepeatMode
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.ui.views.PlayPauseDrawable
import androidx.annotation.MainThread
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import android.content.Context
import android.graphics.PorterDuff.Mode.SRC_IN
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.coroutines.launch

abstract class AbsPlayerControllerFragment<V : ViewBinding> : AbsMusicServiceFragment() {

    protected lateinit var playPauseDrawable: PlayPauseDrawable

    abstract val binding: PlayerControllerBinding<V>

    protected var lastPlaybackControlsColor = 0
    private var lastDisabledPlaybackControlsColor = 0

    private val progressViewUpdateHelperDelegate =
        MusicProgressViewUpdateHelperDelegate(::updateProgressViews)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(progressViewUpdateHelperDelegate)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return binding.inflate(inflater)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.unbind()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpPlayPauseButton()
        setUpPrevNext()
        setUpShuffleButton()
        setUpRepeatButton()

        setUpProgressSlider()

        updateProgressTextColor()
        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                CurrentQueueState.repeatMode.collect { repeatMode ->
                    updateRepeatState(repeatMode)
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                CurrentQueueState.shuffleMode.collect { shuffleMode ->
                    updateShuffleState(shuffleMode)
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                PlayerController.currentState.collect {
                    updatePlayPauseDrawableState(
                        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    )
                }
            }
        }
    }

    abstract fun setUpPlayPauseButton()

    private fun setUpPrevNext() {
        updatePrevNextColor()
        binding.nextButton.setOnClickListener { MusicPlayerRemote.playNextSong() }
        binding.prevButton.setOnClickListener { MusicPlayerRemote.back() }
    }

    private fun setUpShuffleButton() {
        binding.shuffleButton.setOnClickListener { MusicPlayerRemote.toggleShuffleMode() }
    }

    private fun setUpRepeatButton() {
        binding.repeatButton.setOnClickListener { MusicPlayerRemote.cycleRepeatMode() }
    }

    private fun setUpProgressSlider() {
        val color = requireContext().primaryTextColor(true)
        val colorFilter = createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
        binding.progressSlider.thumb.mutate().colorFilter = colorFilter
        binding.progressSlider.progressDrawable.mutate().colorFilter = colorFilter
        binding.progressSlider.setOnSeekBarChangeListener(object : SimpleOnSeekbarChangeListener() {
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
    }

    private fun calculateColor(context: Context, backgroundColor: Int) {
        val darkmode = !isColorLight(backgroundColor)
        lastPlaybackControlsColor = context.secondaryTextColor(darkmode)
        lastDisabledPlaybackControlsColor = context.secondaryDisabledTextColor(darkmode)
    }

    fun modifyColor(backgroundColor: Int) {
        calculateColor(requireContext(), backgroundColor)
        updateAll()
    }

    @MainThread
    private fun updateAll() {
        updateRepeatState(MusicPlayerRemote.repeatMode)
        updateShuffleState(MusicPlayerRemote.shuffleMode)
        updatePrevNextColor()
        updatePlayPauseColor()
        updateProgressTextColor()
    }

    // Update state
    protected abstract fun updatePlayPauseDrawableState(animate: Boolean)
    protected open fun updatePlayPauseColor() {}
    private fun updatePrevNextColor() {
        binding.nextButton.setColorFilter(lastPlaybackControlsColor, SRC_IN)
        binding.prevButton.setColorFilter(lastPlaybackControlsColor, SRC_IN)
    }

    private fun updateProgressTextColor() {
        val color = requireContext().primaryTextColor(true)
        binding.songTotalTime.setTextColor(color)
        binding.songCurrentProgress.setTextColor(color)
    }

    private fun updateProgressViews(progress: Int, total: Int) {
        binding.progressSlider.max = total
        binding.progressSlider.progress = progress
        binding.songTotalTime.text = getReadableDurationString(total.toLong())
        binding.songCurrentProgress.text = getReadableDurationString(progress.toLong())
    }

    private fun updateRepeatState(repeatMode: RepeatMode) = when (repeatMode) {
        RepeatMode.NONE               -> {
            binding.repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp)
            binding.repeatButton.setColorFilter(lastDisabledPlaybackControlsColor, SRC_IN)
        }

        RepeatMode.REPEAT_QUEUE       -> {
            binding.repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp)
            binding.repeatButton.setColorFilter(lastPlaybackControlsColor, SRC_IN)
        }

        RepeatMode.REPEAT_SINGLE_SONG -> {
            binding.repeatButton.setImageResource(R.drawable.ic_repeat_one_white_24dp)
            binding.repeatButton.setColorFilter(lastPlaybackControlsColor, SRC_IN)
        }
    }

    private fun updateShuffleState(shuffleMode: ShuffleMode) = binding.shuffleButton.setColorFilter(
        when (shuffleMode) {
            ShuffleMode.SHUFFLE -> lastPlaybackControlsColor
            ShuffleMode.NONE    -> lastDisabledPlaybackControlsColor
        },
        SRC_IN
    )

    abstract fun show()
    abstract fun hide()


    @Suppress("PropertyName")
    abstract class PlayerControllerBinding<V : ViewBinding> {
        protected var _viewBinding: V? = null
        val viewBinding: V get() = _viewBinding!!

        val isAttached get() = _viewBinding != null


        protected var _prevButton: ImageButton? = null
        val prevButton: ImageButton get() = _prevButton!!
        protected var _nextButton: ImageButton? = null
        val nextButton: ImageButton get() = _nextButton!!

        protected var _repeatButton: ImageButton? = null
        val repeatButton: ImageButton get() = _repeatButton!!
        protected var _shuffleButton: ImageButton? = null
        val shuffleButton: ImageButton get() = _shuffleButton!!

        protected var _progressSlider: SeekBar? = null
        val progressSlider: SeekBar get() = _progressSlider!!
        protected var _songTotalTime: TextView? = null
        val songTotalTime: TextView get() = _songTotalTime!!
        protected var _songCurrentProgress: TextView? = null
        val songCurrentProgress: TextView get() = _songCurrentProgress!!


        abstract fun bind(viewBinding: V)
        abstract fun inflate(inflater: LayoutInflater): View
        open fun unbind() {
            _prevButton = null
            _nextButton = null
            _repeatButton = null
            _shuffleButton = null
            _progressSlider = null
            _songTotalTime = null
            _songCurrentProgress = null
            _viewBinding = null
        }
    }
}
