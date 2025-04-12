/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.player.controller

import com.google.android.material.floatingactionbutton.FloatingActionButton
import player.phonograph.R
import player.phonograph.databinding.FragmentPlaybackControlsClassicBinding
import player.phonograph.ui.modules.player.PlayPauseButtonOnClickHandler
import player.phonograph.ui.views.PlayPauseDrawable
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.themeIconColor
import util.theme.color.secondaryTextColor
import util.theme.view.tint
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import android.animation.AnimatorSet
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView

class PlayerControllerClassicStyledBinding : PlayerControllerBinding() {

    private var _viewBinding: FragmentPlaybackControlsClassicBinding? = null
    val viewBinding: FragmentPlaybackControlsClassicBinding get() = _viewBinding!!

    override fun createView(inflater: LayoutInflater): View {
        _viewBinding = FragmentPlaybackControlsClassicBinding.inflate(inflater)
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

    override val playerPlayPauseButton: FloatingActionButton get() = centralButton

    override val centralButton: FloatingActionButton get() = viewBinding.playerButtonCenter
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
        val fabBackgroundColor = if (context.nightMode) Color.LTGRAY else Color.WHITE
        val fabIconColor = context.secondaryTextColor(fabBackgroundColor)
        playPauseDrawable = PlayPauseDrawable(context).apply {
            colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                fabIconColor, BlendModeCompat.SRC_IN
            )
        }
        disconnectedDrawable = context.getTintedDrawable(R.drawable.ic_refresh_white_24dp, themeIconColor(context))

        with(playerPlayPauseButton) {
            tint(fabBackgroundColor, true, context.nightMode)
            setOnClickListener(PlayPauseButtonOnClickHandler())
            // pivotX = width.toFloat() / 2
            // pivotY = height.toFloat() / 2
        }
    }

    override fun onUpdateColor(oldColor: Int, newColor: Int, withAnimation: Boolean) {
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

    //region Animation
    private var hidden = false
    private var scaleAnimationSet: AnimatorSet? = null

    override fun onShow(withAnimation: Boolean) {
        if (hidden && withAnimation) {
            // Scale
            if (scaleAnimationSet == null) {
                scaleAnimationSet = buildButtonScaleAnimatorSets(withCentralButton = false)
            } else {
                scaleAnimationSet?.end()
                scaleAnimationSet?.cancel()
            }
            scaleAnimationSet?.start()
            // Rotation
            buildRotationAnimator().start()
        } else {
            // Scale
            resetAllButtonsScales(1f)
            // Rotation
            resetRotation(1f, 360f)
        }
        hidden = false
    }

    override fun onHide(withAnimation: Boolean) {
        if (withAnimation) {
            // Scale
            scaleAnimationSet?.cancel()
            resetAllButtonsScales(0f)
            // Rotation
            resetRotation(0f, 0f)
        }
        hidden = true
    }


    private fun resetRotation(scale: Float, angle: Float) {
        playerPlayPauseButton.apply {
            scaleX = scale
            scaleY = scale
            rotation = angle
        }
    }

    private fun buildRotationAnimator(): ViewPropertyAnimator =
        playerPlayPauseButton.animate()
            .scaleX(1f)
            .scaleY(1f)
            .rotation(360f)
            .setInterpolator(DecelerateInterpolator())
    //endregion

}