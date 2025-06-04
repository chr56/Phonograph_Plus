/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.player.controller

import player.phonograph.R
import player.phonograph.model.service.RepeatMode
import player.phonograph.model.service.ShuffleMode
import player.phonograph.model.ui.PlayerControllerStyle.Companion.BUTTONS_PRIMARY
import player.phonograph.model.ui.PlayerControllerStyle.Companion.BUTTONS_SECONDARY
import player.phonograph.model.ui.PlayerControllerStyle.Companion.BUTTONS_TERTIARY
import player.phonograph.model.ui.PlayerControllerStyle.Companion.ButtonPosition
import player.phonograph.model.ui.PlayerControllerStyle.Companion.FUNCTION_NONE
import player.phonograph.model.ui.PlayerControllerStyle.Companion.FUNCTION_QUEUE_MODE_A
import player.phonograph.model.ui.PlayerControllerStyle.Companion.FUNCTION_QUEUE_MODE_N
import player.phonograph.model.ui.PlayerControllerStyle.Companion.FUNCTION_SEEK
import player.phonograph.model.ui.PlayerControllerStyle.Companion.FUNCTION_SWITCH
import player.phonograph.model.ui.PlayerControllerStyle.Companion.FunctionType
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.text.readableDuration
import player.phonograph.util.ui.createScaleAnimator
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.animation.Animator
import android.animation.AnimatorSet
import android.content.Context
import android.graphics.PorterDuff.Mode.SRC_IN
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView

abstract class PlayerControllerBinding {

    abstract fun createView(inflater: LayoutInflater): View
    abstract fun destroyView()

    //region View Accessors
    abstract val progressSlider: SeekBar
    abstract val songTotalTime: TextView
    abstract val songCurrentProgress: TextView

    abstract val playerPlayPauseButton: ImageButton

    //region Geomagnetic Buttons Accessor
    abstract val centralButton: ImageButton
    abstract val centralButtonBox: View?

    abstract val primaryButtonLeft: ImageButton
    abstract val primaryButtonRight: ImageButton

    abstract val secondaryButtonLeft: ImageButton
    abstract val secondaryButtonRight: ImageButton

    abstract val tertiaryButtonLeft: ImageButton
    abstract val tertiaryButtonRight: ImageButton
    //endregion

    //region Semantic Buttons Accessor

    var prevButton: ImageButton? = null
        protected set
    var nextButton: ImageButton? = null
        protected set

    var rewindButton: ImageButton? = null
        protected set
    var forwardButton: ImageButton? = null
        protected set

    var repeatButton: ImageButton? = null
        protected set
    var shuffleButton: ImageButton? = null
        protected set

    protected val unusedButtons = mutableListOf<ImageButton>()
    //endregion

    abstract val root: View

    //endregion

    //region Functions
    fun designate(@FunctionType function: Int, @ButtonPosition position: Int) {
        val left = findButtonAt(position, true)
        val right = findButtonAt(position, false)
        when (function) {
            FUNCTION_SWITCH       -> {
                prevButton = left
                nextButton = right
                if (left != null) unusedButtons.remove(left)
                if (right != null) unusedButtons.remove(right)
            }

            FUNCTION_SEEK         -> {
                rewindButton = left
                forwardButton = right
                if (left != null) unusedButtons.remove(left)
                if (right != null) unusedButtons.remove(right)
            }

            FUNCTION_QUEUE_MODE_N -> {
                repeatButton = left
                shuffleButton = right
                if (left != null) unusedButtons.remove(left)
                if (right != null) unusedButtons.remove(right)
            }

            FUNCTION_QUEUE_MODE_A -> {
                shuffleButton = left
                repeatButton = right
                if (left != null) unusedButtons.remove(left)
                if (right != null) unusedButtons.remove(right)
            }

            FUNCTION_NONE         -> {
                if (left != null) unusedButtons.add(left)
                if (right != null) unusedButtons.add(right)
            }
        }
    }

    fun reset() {
        prevButton = null
        nextButton = null
        rewindButton = null
        forwardButton = null
        repeatButton = null
        shuffleButton = null
        unusedButtons.add(primaryButtonLeft)
        unusedButtons.add(primaryButtonRight)
        unusedButtons.add(secondaryButtonLeft)
        unusedButtons.add(secondaryButtonRight)
        unusedButtons.add(tertiaryButtonLeft)
        unusedButtons.add(tertiaryButtonRight)
    }

    private fun findButtonAt(@ButtonPosition position: Int, leftOrRight: Boolean): ImageButton? =
        when (position) {
            BUTTONS_PRIMARY   -> if (leftOrRight) primaryButtonLeft else primaryButtonRight
            BUTTONS_SECONDARY -> if (leftOrRight) secondaryButtonLeft else secondaryButtonRight
            BUTTONS_TERTIARY  -> if (leftOrRight) tertiaryButtonLeft else tertiaryButtonRight
            else              -> null
        }

    fun commit(context: Context) {

        prevButton?.visibility = View.VISIBLE
        nextButton?.visibility = View.VISIBLE
        prevButton?.setImageResource(R.drawable.ic_skip_previous_white_24dp)
        nextButton?.setImageResource(R.drawable.ic_skip_next_white_24dp)
        prevButton?.contentDescription = context.getString(R.string.action_previous)
        nextButton?.contentDescription = context.getString(R.string.action_next)
        prevButton?.setOnClickListener { MusicPlayerRemote.back() }
        nextButton?.setOnClickListener { MusicPlayerRemote.playNextSong() }

        rewindButton?.visibility = View.VISIBLE
        forwardButton?.visibility = View.VISIBLE
        rewindButton?.setImageResource(R.drawable.ic_fast_rewind_white_24dp)
        forwardButton?.setImageResource(R.drawable.ic_fast_forward_white_24dp)
        rewindButton?.contentDescription = context.getString(R.string.action_fast_rewind)
        forwardButton?.contentDescription = context.getString(R.string.action_fast_forward)
        rewindButton?.setOnClickListener { MusicPlayerRemote.fastRewind() }
        forwardButton?.setOnClickListener { MusicPlayerRemote.fastForward() }

        shuffleButton?.visibility = View.VISIBLE
        repeatButton?.visibility = View.VISIBLE
        shuffleButton?.setImageResource(R.drawable.ic_shuffle_white_24dp)
        repeatButton?.setImageResource(R.drawable.ic_repeat_white_24dp)
        shuffleButton?.contentDescription = context.getString(R.string.action_shuffle_mode)
        repeatButton?.contentDescription = context.getString(R.string.action_repeat_mode)
        shuffleButton?.setOnClickListener { MusicPlayerRemote.toggleShuffleMode() }
        repeatButton?.setOnClickListener { MusicPlayerRemote.cycleRepeatMode() }

        for (unused in unusedButtons) {
            unused.visibility = View.GONE
            unused.setImageDrawable(null)
            unused.setOnClickListener(null)
            unused.contentDescription = null
        }
    }

    abstract fun setupPlayPauseButton(context: Context)

    fun setUpProgressSlider() {
        progressSlider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) MusicPlayerRemote.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }
    //endregion


    //region State Update

    abstract fun onUpdatePlayerState(@PlayerState newState: Int, withAnimation: Boolean)

    abstract fun onUpdateColor(oldColor: Int, newColor: Int, withAnimation: Boolean)


    fun onUpdateProgressViews(progress: Int, total: Int) {
        progressSlider.max = total
        progressSlider.progress = progress
        songTotalTime.text = readableDuration(total.toLong())
        songCurrentProgress.text = readableDuration(progress.toLong())
    }

    fun onUpdateRepeatModeIcon(repeatMode: RepeatMode) =
        repeatButton?.setImageResource(
            when (repeatMode) {
                RepeatMode.NONE               -> R.drawable.ic_repeat_off_white_24dp
                RepeatMode.REPEAT_QUEUE       -> R.drawable.ic_repeat_white_24dp
                RepeatMode.REPEAT_SINGLE_SONG -> R.drawable.ic_repeat_one_white_24dp
            }
        )

    fun onUpdateShuffleModeIcon(shuffleMode: ShuffleMode) =
        shuffleButton?.setImageResource(
            when (shuffleMode) {
                ShuffleMode.NONE    -> R.drawable.ic_shuffle_disabled_white_24dp
                ShuffleMode.SHUFFLE -> R.drawable.ic_shuffle_white_24dp
            }
        )


    abstract fun onShow(withAnimation: Boolean)

    abstract fun onHide(withAnimation: Boolean)
    //endregion


    //region Colors
    fun setupProgressBarTextColor(@ColorInt color: Int) {
        val colorFilter = createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)
        progressSlider.thumb.mutate().colorFilter = colorFilter
        progressSlider.progressDrawable.mutate().colorFilter = colorFilter
        songTotalTime.setTextColor(color)
        songCurrentProgress.setTextColor(color)
    }

    fun updateButtonsColor(@ColorInt color: Int) {
        primaryButtonLeft.setColorFilter(color, SRC_IN)
        primaryButtonRight.setColorFilter(color, SRC_IN)

        secondaryButtonLeft.setColorFilter(color, SRC_IN)
        secondaryButtonRight.setColorFilter(color, SRC_IN)

        tertiaryButtonLeft.setColorFilter(color, SRC_IN)
        tertiaryButtonRight.setColorFilter(color, SRC_IN)
    }
    //endregion


    //region Animation

    protected fun buildButtonScaleAnimatorSets(
        withCentralButton: Boolean = true,
        withPrimaryButtons: Boolean = true,
        withSecondaryButtons: Boolean = true,
        withTertiaryButtons: Boolean = true,
    ): AnimatorSet {
        return AnimatorSet().apply {
            val defaultInterpolator = FastOutSlowInInterpolator()
            val animatorSet = mutableListOf<Animator>()

            var delay = 0L
            // Central button
            if (withCentralButton) {
                animatorSet.add(createScaleAnimator(centralButton, true, defaultInterpolator, delay))
                animatorSet.add(createScaleAnimator(centralButton, false, defaultInterpolator, delay))
                delay += 75
            }

            // Primary buttons
            if (withPrimaryButtons) {
                animatorSet.add(createScaleAnimator(primaryButtonLeft, true, defaultInterpolator, delay))
                animatorSet.add(createScaleAnimator(primaryButtonLeft, false, defaultInterpolator, delay))
                animatorSet.add(createScaleAnimator(primaryButtonRight, true, defaultInterpolator, delay))
                animatorSet.add(createScaleAnimator(primaryButtonRight, false, defaultInterpolator, delay))
                delay += 75
            }

            // Secondary buttons
            if (withSecondaryButtons) {
                animatorSet.add(createScaleAnimator(secondaryButtonLeft, true, defaultInterpolator, delay))
                animatorSet.add(createScaleAnimator(secondaryButtonLeft, false, defaultInterpolator, delay))
                animatorSet.add(createScaleAnimator(secondaryButtonRight, true, defaultInterpolator, delay))
                animatorSet.add(createScaleAnimator(secondaryButtonRight, false, defaultInterpolator, delay))
                delay += 75
            }

            // Tertiary buttons
            if (withTertiaryButtons) {
                animatorSet.add(createScaleAnimator(tertiaryButtonLeft, true, defaultInterpolator, delay))
                animatorSet.add(createScaleAnimator(tertiaryButtonLeft, false, defaultInterpolator, delay))
                animatorSet.add(createScaleAnimator(tertiaryButtonRight, true, defaultInterpolator, delay))
                animatorSet.add(createScaleAnimator(tertiaryButtonRight, false, defaultInterpolator, delay))
                delay += 75
            }

            playTogether(animatorSet)
        }
    }

    protected fun resetAllButtonsScales(scale: Float) {
        setScale(playerPlayPauseButton, scale)
        setScale(prevButton, scale)
        setScale(nextButton, scale)
        setScale(rewindButton, scale)
        setScale(forwardButton, scale)
        setScale(repeatButton, scale)
        setScale(shuffleButton, scale)
    }

    protected fun setScale(view: View?, scale: Float) {
        if (view != null) {
            view.scaleX = scale
            view.scaleY = scale
        }
    }
    //endregion


    companion object {


        const val STATE_STOPPED = 0
        const val STATE_PLAYING = 1
        const val STATE_PAUSED = 2
        const val STATE_BUFFERING = 3

        @IntDef(STATE_STOPPED, STATE_PLAYING, STATE_PAUSED, STATE_BUFFERING)
        @Retention(AnnotationRetention.SOURCE)
        annotation class PlayerState
    }


}