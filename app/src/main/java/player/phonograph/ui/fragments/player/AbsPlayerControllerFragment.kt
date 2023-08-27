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
import androidx.annotation.ColorInt
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

    abstract val binding: PlayerControllerBinding<V>

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
        val context = view.context
        super.onViewCreated(view, savedInstanceState)

        binding.setUpPrevButton { MusicPlayerRemote.back() }
        binding.setUpNextButton { MusicPlayerRemote.playNextSong() }
        binding.setUpShuffleButton { MusicPlayerRemote.toggleShuffleMode() }
        binding.setUpRepeatButton { MusicPlayerRemote.cycleRepeatMode() }

        calculateColor(context, context.getColor(R.color.defaultFooterColor))

        binding.setUpPlayPauseButton(context)
        binding.updatePlayPauseColor(controlsColor)
        binding.updatePrevNextColor(controlsColor)

        binding.setUpProgressSlider(primaryTextColor)
        binding.updateProgressTextColor(primaryTextColor)

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                CurrentQueueState.repeatMode.collect { repeatMode ->
                    binding.updateRepeatState(repeatMode, controlsColor, disabledControlsColor)
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                CurrentQueueState.shuffleMode.collect { shuffleMode ->
                    binding.updateShuffleState(shuffleMode, controlsColor, disabledControlsColor)
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                PlayerController.currentState.collect {
                    binding.updatePlayPauseDrawableState(
                        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    )
                }
            }
        }
    }

    private var primaryTextColor = 0
    private var controlsColor = 0
    private var disabledControlsColor = 0
    private fun calculateColor(context: Context, backgroundColor: Int) {
        val darkmode = !isColorLight(backgroundColor)
        primaryTextColor = context.primaryTextColor(darkmode)
        controlsColor = context.secondaryTextColor(darkmode)
        disabledControlsColor = context.secondaryDisabledTextColor(darkmode)
    }

    fun modifyColor(backgroundColor: Int) {
        calculateColor(requireContext(), backgroundColor)
        refreshAll()
    }

    @MainThread
    private fun refreshAll() {
        binding.updatePlayPauseColor(controlsColor)
        binding.updateRepeatState(MusicPlayerRemote.repeatMode, controlsColor, disabledControlsColor)
        binding.updateShuffleState(MusicPlayerRemote.shuffleMode, controlsColor, disabledControlsColor)
        binding.updatePrevNextColor(controlsColor)
        binding.updateProgressTextColor(primaryTextColor)
    }

    private fun updateProgressViews(progress: Int, total: Int) = binding.updateProgressViews(progress, total)

    //endregion

    abstract fun show()
    abstract fun hide()


    @Suppress("PropertyName")
    abstract class PlayerControllerBinding<V : ViewBinding> {

        //region Binding
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
        //endregion


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


        //region Color
        fun updatePrevNextColor(@ColorInt color: Int) {
            nextButton.setColorFilter(color, SRC_IN)
            prevButton.setColorFilter(color, SRC_IN)
        }

        fun updateRepeatState(repeatMode: RepeatMode, @ColorInt color: Int, @ColorInt disabledColor: Int) =
            when (repeatMode) {
                RepeatMode.NONE               -> {
                    repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp)
                    repeatButton.setColorFilter(disabledColor, SRC_IN)
                }

                RepeatMode.REPEAT_QUEUE       -> {
                    repeatButton.setImageResource(R.drawable.ic_repeat_white_24dp)
                    repeatButton.setColorFilter(color, SRC_IN)
                }

                RepeatMode.REPEAT_SINGLE_SONG -> {
                    repeatButton.setImageResource(R.drawable.ic_repeat_one_white_24dp)
                    repeatButton.setColorFilter(color, SRC_IN)
                }
            }

        fun updateShuffleState(shuffleMode: ShuffleMode, @ColorInt color: Int, @ColorInt disabledColor: Int) =
            shuffleButton.setColorFilter(
                when (shuffleMode) {
                    ShuffleMode.SHUFFLE -> color
                    ShuffleMode.NONE    -> disabledColor
                },
                SRC_IN
            )

        fun updateProgressTextColor(color: Int) {
            songTotalTime.setTextColor(color)
            songCurrentProgress.setTextColor(color)
        }
        //endregion

        //region Text
        fun updateProgressViews(progress: Int, total: Int) {
            progressSlider.max = total
            progressSlider.progress = progress
            songTotalTime.text = getReadableDurationString(total.toLong())
            songCurrentProgress.text = getReadableDurationString(progress.toLong())
        }
        //endregion


        //region Behaviour
        lateinit var playPauseDrawable: PlayPauseDrawable
        abstract fun setUpPlayPauseButton(context: Context)
        abstract fun updatePlayPauseColor(controlsColor: Int)

        fun updatePlayPauseDrawableState(animate: Boolean) {
            if (MusicPlayerRemote.isPlaying) {
                playPauseDrawable.setPause(animate)
            } else {
                playPauseDrawable.setPlay(animate)
            }
        }

        fun setUpPrevButton(onClickListener: View.OnClickListener) {
            prevButton.setOnClickListener(onClickListener)
        }

        fun setUpNextButton(onClickListener: View.OnClickListener) {
            nextButton.setOnClickListener(onClickListener)
        }

        fun setUpShuffleButton(onClickListener: View.OnClickListener) {
            shuffleButton.setOnClickListener(onClickListener)
        }

        fun setUpRepeatButton(onClickListener: View.OnClickListener) {
            repeatButton.setOnClickListener(onClickListener)
        }

        fun setUpProgressSlider(color: Int) {
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
        //endregion
    }
}
