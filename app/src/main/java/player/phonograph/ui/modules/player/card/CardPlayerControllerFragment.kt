/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.modules.player.card

import com.google.android.material.floatingactionbutton.FloatingActionButton
import player.phonograph.databinding.FragmentCardPlayerPlaybackControlsBinding
import player.phonograph.ui.modules.player.AbsPlayerControllerFragment
import player.phonograph.ui.modules.player.PlayPauseButtonOnClickHandler
import player.phonograph.ui.views.PlayPauseDrawable
import player.phonograph.util.theme.nightMode
import util.theme.color.secondaryTextColor
import util.theme.view.tint
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator

class CardPlayerControllerFragment : AbsPlayerControllerFragment<FragmentCardPlayerPlaybackControlsBinding>() {

    var progressSliderHeight: Int = -1
        private set

    override val binding: CardPlayerControllerBinding = CardPlayerControllerBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progressSliderHeight = binding.viewBinding.playerProgressSlider.height
        super.onViewCreated(view, savedInstanceState)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        progressSliderHeight = -1
    }

    val playerPlayPauseFab get() = binding.playerPlayPauseFab

    override fun show() {
        if (isResumed) binding.viewBinding.playerPlayPauseFab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .rotation(360f)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun hide() {
        if (isResumed) binding.viewBinding.playerPlayPauseFab.apply {
            scaleX = 0f
            scaleY = 0f
            rotation = 0f
        }
    }


    class CardPlayerControllerBinding : PlayerControllerBinding<FragmentCardPlayerPlaybackControlsBinding>() {
        private var _playerPlayPauseFab: FloatingActionButton? = null
        val playerPlayPauseFab: FloatingActionButton get() = _playerPlayPauseFab!!

        override fun bind(viewBinding: FragmentCardPlayerPlaybackControlsBinding) {
            _prevButton = viewBinding.playerPrevButton
            _nextButton = viewBinding.playerNextButton
            _repeatButton = viewBinding.playerRepeatButton
            _shuffleButton = viewBinding.playerShuffleButton
            _progressSlider = viewBinding.playerProgressSlider
            _songCurrentProgress = viewBinding.playerSongCurrentProgress
            _songTotalTime = viewBinding.playerSongTotalTime

            _playerPlayPauseFab = viewBinding.playerPlayPauseFab
        }

        override fun inflate(inflater: LayoutInflater): View {
            _viewBinding = FragmentCardPlayerPlaybackControlsBinding.inflate(inflater)
            bind(viewBinding)
            return viewBinding.root
        }

        override fun unbind() {
            _playerPlayPauseFab = null
            super.unbind()
        }

        override fun preparePlayPauseButton(context: Context) {
            val fabColor = Color.WHITE
            playPauseDrawable = PlayPauseDrawable(context).apply {
                colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                        context.secondaryTextColor(fabColor), BlendModeCompat.SRC_IN
                    )
            }
            with(viewBinding.playerPlayPauseFab) {
                tint(fabColor, true, context.nightMode)
                setOnClickListener(PlayPauseButtonOnClickHandler())
                // pivotX = width.toFloat() / 2
                // pivotY = height.toFloat() / 2
            }
        }

        override fun setPlayPauseButton(drawable: Drawable?) {
            viewBinding.playerPlayPauseFab.setImageDrawable(drawable)
        }

        override fun updatePlayPauseColor(controlsColor: Int) {}

    }
}
