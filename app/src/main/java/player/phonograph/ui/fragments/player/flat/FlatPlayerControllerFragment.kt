/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.player.flat

import player.phonograph.databinding.FragmentFlatPlayerPlaybackControlsBinding
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.fragments.player.AbsPlayerControllerFragment
import player.phonograph.ui.fragments.player.PlayPauseButtonOnClickHandler
import player.phonograph.ui.views.PlayPauseDrawable
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import java.util.*

class FlatPlayerControllerFragment : AbsPlayerControllerFragment<FragmentFlatPlayerPlaybackControlsBinding>() {

    override val binding: FlatPlayerControllerBinding = FlatPlayerControllerBinding()


    override fun setUpPlayPauseButton() {
        playPauseDrawable = PlayPauseDrawable(requireActivity())
        binding.viewBinding.playerPlayPauseButton.setImageDrawable(playPauseDrawable)
        updatePlayPauseColor()
        binding.viewBinding.playerPlayPauseButton.setOnClickListener(PlayPauseButtonOnClickHandler())
        binding.viewBinding.playerPlayPauseButton.post {
            // viewBinding might be null, such as when resizing windows
            binding.viewBinding?.let { binding ->
                binding.playerPlayPauseButton.pivotX = binding.playerPlayPauseButton.width.toFloat() / 2
                binding.playerPlayPauseButton.pivotY = binding.playerPlayPauseButton.height.toFloat() / 2
            }
        }
    }

    override fun updatePlayPauseDrawableState(animate: Boolean) {
        if (MusicPlayerRemote.isPlaying) {
            playPauseDrawable.setPause(animate)
        } else {
            playPauseDrawable.setPlay(animate)
        }
    }

    override fun updatePlayPauseColor() {
        binding.viewBinding.playerPlayPauseButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
    }

    private var hidden = false
    private var musicControllerAnimationSet: AnimatorSet? = null

    override fun show() {
        if (hidden && isResumed) {
            if (musicControllerAnimationSet == null) {
                val interpolator: TimeInterpolator = FastOutSlowInInterpolator()
                val duration = 300
                val animators = LinkedList<Animator>()
                addAnimation(animators, binding.viewBinding.playerPlayPauseButton, interpolator, duration, 0)
                addAnimation(animators, binding.viewBinding.playerNextButton, interpolator, duration, 100)
                addAnimation(animators, binding.viewBinding.playerPrevButton, interpolator, duration, 100)
                addAnimation(animators, binding.viewBinding.playerRepeatButton, interpolator, duration, 200)
                addAnimation(animators, binding.viewBinding.playerShuffleButton, interpolator, duration, 200)
                musicControllerAnimationSet = AnimatorSet()
                musicControllerAnimationSet!!.playTogether(animators)
            } else {
                musicControllerAnimationSet!!.cancel()
            }
            musicControllerAnimationSet!!.start()
        }
        hidden = false
    }

    override fun hide() {
        musicControllerAnimationSet?.cancel()
        if (isResumed) {
            prepareForAnimation(binding.viewBinding.playerPlayPauseButton)
            prepareForAnimation(binding.viewBinding.playerNextButton)
            prepareForAnimation(binding.viewBinding.playerPrevButton)
            prepareForAnimation(binding.viewBinding.playerRepeatButton)
            prepareForAnimation(binding.viewBinding.playerShuffleButton)
        }
        hidden = true
    }

    @Suppress("SameParameterValue")
    private fun addAnimation(
        animators: MutableCollection<Animator>,
        view: View,
        interpolator: TimeInterpolator,
        duration: Int,
        delay: Int,
    ) {
        val scaleX: Animator = ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1f)
        scaleX.interpolator = interpolator
        scaleX.duration = duration.toLong()
        scaleX.startDelay = delay.toLong()
        animators.add(scaleX)
        val scaleY: Animator = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f)
        scaleY.interpolator = interpolator
        scaleY.duration = duration.toLong()
        scaleY.startDelay = delay.toLong()
        animators.add(scaleY)
    }

    private fun prepareForAnimation(view: View?) {
        if (view != null) {
            view.scaleX = 0f
            view.scaleY = 0f
        }
    }

    class FlatPlayerControllerBinding : PlayerControllerBinding<FragmentFlatPlayerPlaybackControlsBinding>() {

        override fun bind(viewBinding: FragmentFlatPlayerPlaybackControlsBinding) {
            _prevButton = viewBinding.playerPrevButton
            _nextButton = viewBinding.playerNextButton
            _repeatButton = viewBinding.playerRepeatButton
            _shuffleButton = viewBinding.playerShuffleButton
            _progressSlider = viewBinding.playerProgressSlider
            _songCurrentProgress = viewBinding.playerSongCurrentProgress
            _songTotalTime = viewBinding.playerSongTotalTime
        }

        override fun inflate(inflater: LayoutInflater): View {
            _viewBinding = FragmentFlatPlayerPlaybackControlsBinding.inflate(inflater)
            bind(viewBinding)
            return viewBinding.root
        }
    }
}
