package player.phonograph.misc

import android.animation.Animator

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
abstract class SimpleAnimatorListener : Animator.AnimatorListener {
    override fun onAnimationStart(animation: Animator) {}
    override fun onAnimationEnd(animation: Animator) {}
    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}
}
