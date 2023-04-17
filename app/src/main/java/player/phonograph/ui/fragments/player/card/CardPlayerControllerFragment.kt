/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.fragments.player.card

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mt.tint.viewtint.tint
import mt.util.color.secondaryTextColor
import player.phonograph.databinding.FragmentCardPlayerPlaybackControlsBinding
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.fragments.player.AbsPlayerControllerFragment
import player.phonograph.ui.fragments.player.PlayPauseButtonOnClickHandler
import player.phonograph.ui.views.PlayPauseDrawable

class CardPlayerControllerFragment : AbsPlayerControllerFragment() {

    private var viewBinding: FragmentCardPlayerPlaybackControlsBinding? = null
    private val v: FragmentCardPlayerPlaybackControlsBinding get() = viewBinding!!

    private var _playerPlayPauseFab: FloatingActionButton? = null
    val playerPlayPauseFab: FloatingActionButton get() = _playerPlayPauseFab!!
    var progressSliderHeight: Int = -1
        private set

    override fun bindView(inflater: LayoutInflater): View {
        viewBinding = FragmentCardPlayerPlaybackControlsBinding.inflate(inflater)
        _playerPlayPauseFab = v.playerPlayPauseFab
        prevButton = v.playerPrevButton
        nextButton = v.playerNextButton
        repeatButton = v.playerRepeatButton
        shuffleButton = v.playerShuffleButton
        progressSlider = v.playerProgressSlider
        songCurrentProgress = v.playerSongCurrentProgress
        songTotalTime = v.playerSongTotalTime
        return v.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progressSliderHeight = v.playerProgressSlider.height
        super.onViewCreated(view, savedInstanceState)
    }

    override fun unbindView() {
        viewBinding = null
        _playerPlayPauseFab = null
        progressSliderHeight = -1
    }

    override fun setUpPlayPauseButton() {
        val fabColor = Color.WHITE
        v.playerPlayPauseFab.tint(fabColor, true)

        playPauseDrawable = PlayPauseDrawable(requireActivity())
        playPauseDrawable.colorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                requireContext().secondaryTextColor(fabColor), BlendModeCompat.SRC_IN
            )

        v.playerPlayPauseFab.setImageDrawable(playPauseDrawable) // Note: set the drawable AFTER TintHelper.setTintAuto() was called

        v.playerPlayPauseFab.setOnClickListener(PlayPauseButtonOnClickHandler())
        v.playerPlayPauseFab.post {
            // viewBinding might be null, such as when resizing windows
            viewBinding?.let { binding ->
                binding.playerPlayPauseFab.pivotX = binding.playerPlayPauseFab.width.toFloat() / 2
                binding.playerPlayPauseFab.pivotY = binding.playerPlayPauseFab.height.toFloat() / 2
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

    override fun show() {
        if (isResumed) v.playerPlayPauseFab.animate()
            .scaleX(1f)
            .scaleY(1f)
            .rotation(360f)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun hide() {
        if (isResumed) v.playerPlayPauseFab.apply {
            scaleX = 0f
            scaleY = 0f
            rotation = 0f
        }
    }
}
