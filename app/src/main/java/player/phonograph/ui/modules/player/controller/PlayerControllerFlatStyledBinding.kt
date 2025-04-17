/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.player.controller

import player.phonograph.R
import player.phonograph.databinding.FragmentPlaybackControlsFlatBinding
import player.phonograph.ui.modules.player.PlayPauseButtonOnClickHandler
import player.phonograph.ui.views.PlayPauseDrawable
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.themeIconColor
import android.animation.AnimatorSet
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView

class PlayerControllerFlatStyledBinding : PlayerControllerBinding() {

    private var _viewBinding: FragmentPlaybackControlsFlatBinding? = null
    val viewBinding: FragmentPlaybackControlsFlatBinding get() = _viewBinding!!

    override fun createView(inflater: LayoutInflater): View {
        _viewBinding = FragmentPlaybackControlsFlatBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun destroyView() {
        prevButton = null
        nextButton = null
        rewindButton = null
        forwardButton = null
        repeatButton = null
        shuffleButton = null
        unusedButtons.clear()

        playPauseDrawable = null
        disconnectedDrawable = null

        _viewBinding = null
    }

    //region View Accessor
    override val progressSlider: SeekBar get() = viewBinding.playerProgressSlider
    override val songTotalTime: TextView get() = viewBinding.playerSongTotalTime
    override val songCurrentProgress: TextView get() = viewBinding.playerSongCurrentProgress

    override val playerPlayPauseButton: ImageButton get() = centralButton

    override val centralButton: ImageButton get() = viewBinding.playerButtonCenter
    override val centralButtonBox: View? get() = null

    override val primaryButtonLeft: ImageButton
        get() = viewBinding.playerButtonPrimaryLeft
    override val primaryButtonRight: ImageButton
        get() = viewBinding.playerButtonPrimaryRight
    override val secondaryButtonLeft: ImageButton
        get() = viewBinding.playerButtonSecondaryLeft
    override val secondaryButtonRight: ImageButton
        get() = viewBinding.playerButtonSecondaryRight
    override val tertiaryButtonLeft: ImageButton
        get() = viewBinding.playerButtonTertiaryLeft
    override val tertiaryButtonRight: ImageButton
        get() = viewBinding.playerButtonTertiaryRight

    override val root: View get() = viewBinding.root
    //endregion

    private var playPauseDrawable: PlayPauseDrawable? = null
    private var disconnectedDrawable: Drawable? = null
    override fun setupPlayPauseButton(context: Context) {
        playPauseDrawable = PlayPauseDrawable(context)
        disconnectedDrawable = context.getTintedDrawable(R.drawable.ic_refresh_white_24dp, themeIconColor(context))
        with(playerPlayPauseButton) {
            setOnClickListener(PlayPauseButtonOnClickHandler())
            // pivotX = width.toFloat() / 2
            // pivotY = height.toFloat() / 2
        }
    }

    override fun onUpdateColor(oldColor: Int, newColor: Int, withAnimation: Boolean) {
        playerPlayPauseButton.setColorFilter(newColor, PorterDuff.Mode.SRC_IN)
        updateButtonsColor(newColor)
    }

    override fun onUpdatePlayerState(newState: Int, withAnimation: Boolean) {
        when (newState) {

            STATE_PAUSED    -> {
                playerPlayPauseButton.setImageDrawable(playPauseDrawable)
                playPauseDrawable?.update(true, withAnimation)
            }

            STATE_PLAYING   -> {
                playerPlayPauseButton.setImageDrawable(playPauseDrawable)
                playPauseDrawable?.update(false, withAnimation)
            }

            STATE_STOPPED   -> {
                playerPlayPauseButton.setImageDrawable(disconnectedDrawable)
                playPauseDrawable?.update(true, withAnimation)
            }

            STATE_BUFFERING -> {
                playerPlayPauseButton.setImageDrawable(playPauseDrawable)
                playPauseDrawable?.update(true, withAnimation)
            }
        }

    }


    //region Animations

    private var hidden = false
    private var scaleAnimationSet: AnimatorSet? = null

    override fun onShow(withAnimation: Boolean) {
        if (hidden && withAnimation) {
            if (scaleAnimationSet == null) {
                scaleAnimationSet = buildButtonScaleAnimatorSets()
            } else {
                scaleAnimationSet?.end()
                scaleAnimationSet?.cancel()
            }
            scaleAnimationSet?.start()
        } else {
            resetAllButtonsScales(1f)
        }
        hidden = false
    }

    override fun onHide(withAnimation: Boolean) {
        if (withAnimation) {
            scaleAnimationSet?.cancel()
            resetAllButtonsScales(0f)
        }
        hidden = true
    }

    //endregion
}