/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.util.ui

import androidx.annotation.ColorInt
import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.TextView

const val PHONOGRAPH_ANIM_TIME = 1000L

abstract class SimpleAnimatorListener : Animator.AnimatorListener {
    override fun onAnimationStart(animation: Animator) {}
    override fun onAnimationEnd(animation: Animator) {}
    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}
}

fun View.backgroundColorTransitionAnimator(
    @ColorInt startColor: Int,
    @ColorInt endColor: Int,
): Animator = createColorAnimator(this, "backgroundColor", startColor, endColor)

fun TextView.textColorTransitionAnimator(
    @ColorInt startColor: Int,
    @ColorInt endColor: Int,
): Animator = createColorAnimator(this, "textColor", startColor, endColor)

@SuppressLint("ObsoleteSdkInt")
private fun createColorAnimator(
    target: Any,
    propertyName: String,
    @ColorInt startColor: Int,
    @ColorInt endColor: Int,
): Animator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ObjectAnimator.ofArgb(target, propertyName, startColor, endColor).also {
            it.interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
        }
    } else {
        ObjectAnimator.ofInt(target, propertyName, startColor, endColor).also {
            it.setEvaluator(ArgbEvaluator())
        }
    }.apply {
        duration = PHONOGRAPH_ANIM_TIME
    }

fun createScaleAnimator(
    target: View,
    axis: Boolean,
    interpolator: TimeInterpolator,
    delay: Long = 0,
    duration: Long = PHONOGRAPH_ANIM_TIME / 3,
): Animator =
    ObjectAnimator.ofFloat(
        target, if (axis) View.SCALE_X else View.SCALE_Y,
        0f, 1f
    ).also {
        it.interpolator = interpolator
        it.startDelay = delay
        it.duration = duration
    }

fun ValueAnimator.setupValueAnimator(
    interpolator: TimeInterpolator = PathInterpolator(0.4f, 0f, 1f, 1f),
    duration: Long = PHONOGRAPH_ANIM_TIME / 2,
    onUpdate: (ValueAnimator) -> Unit,
): ValueAnimator = also { animator ->
    animator.duration = duration
    animator.interpolator = interpolator
    animator.addUpdateListener(onUpdate)
}