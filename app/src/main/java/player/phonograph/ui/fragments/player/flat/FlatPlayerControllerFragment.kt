/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.player.flat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import player.phonograph.databinding.FragmentFlatPlayerPlaybackControlsBinding
import player.phonograph.misc.PlayPauseButtonOnClickHandler
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.fragments.player.AbsPlayerControllerFragment
import player.phonograph.views.PlayPauseDrawable
import java.util.*

class FlatPlayerControllerFragment : AbsPlayerControllerFragment() {

    private var viewBinding: FragmentFlatPlayerPlaybackControlsBinding? = null
    private val v: FragmentFlatPlayerPlaybackControlsBinding get() = viewBinding!!

    override fun bindView(inflater: LayoutInflater): View {
        viewBinding = FragmentFlatPlayerPlaybackControlsBinding.inflate(inflater)
        prevButton = v.playerPrevButton
        nextButton = v.playerNextButton
        repeatButton = v.playerRepeatButton
        shuffleButton = v.playerShuffleButton
        progressSlider = v.playerProgressSlider
        songCurrentProgress = v.playerSongCurrentProgress
        songTotalTime = v.playerSongTotalTime
        return v.root
    }

    override fun unbindView() {
        viewBinding = null
    }

    override fun setUpPlayPauseButton() {
        playPauseDrawable = PlayPauseDrawable(requireActivity())
        v.playerPlayPauseButton.setImageDrawable(playPauseDrawable)
        updatePlayPauseColor()
        v.playerPlayPauseButton.setOnClickListener(PlayPauseButtonOnClickHandler())
        v.playerPlayPauseButton.post {
            // viewBinding might be null, such as when resizing windows
            viewBinding?.let { binding ->
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
        v.playerPlayPauseButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
    }

    private var hidden = false
    private var musicControllerAnimationSet: AnimatorSet? = null

    override fun show() {
        if (hidden && isResumed) {
            if (musicControllerAnimationSet == null) {
                val interpolator: TimeInterpolator = FastOutSlowInInterpolator()
                val duration = 300
                val animators = LinkedList<Animator>()
                addAnimation(animators, v.playerPlayPauseButton, interpolator, duration, 0)
                addAnimation(animators, v.playerNextButton, interpolator, duration, 100)
                addAnimation(animators, v.playerPrevButton, interpolator, duration, 100)
                addAnimation(animators, v.playerRepeatButton, interpolator, duration, 200)
                addAnimation(animators, v.playerShuffleButton, interpolator, duration, 200)
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
            prepareForAnimation(v.playerPlayPauseButton)
            prepareForAnimation(v.playerNextButton)
            prepareForAnimation(v.playerPrevButton)
            prepareForAnimation(v.playerRepeatButton)
            prepareForAnimation(v.playerShuffleButton)
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
}
