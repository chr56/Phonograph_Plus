package player.phonograph.ui.fragments.player

import mt.pref.ThemeColor
import mt.util.color.resolveColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.databinding.FragmentMiniPlayerBinding
import player.phonograph.service.queue.CurrentQueueState
import player.phonograph.misc.MusicProgressViewUpdateHelperDelegate
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.player.PlayerController
import player.phonograph.service.player.currentState
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.ui.views.PlayPauseDrawable
import player.phonograph.util.theme.nightMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class MiniPlayerFragment : AbsMusicServiceFragment() {
    private var viewBinding: FragmentMiniPlayerBinding? = null
    private val binding get() = viewBinding!!

    private var miniPlayerPlayPauseDrawable: PlayPauseDrawable? = null

    private val progressViewUpdateHelperDelegate =
        MusicProgressViewUpdateHelperDelegate(::updateProgressViews)

    override fun onCreate(savedInstanceState: Bundle?) {
        viewBinding = FragmentMiniPlayerBinding.inflate(layoutInflater)
        lifecycle.addObserver(progressViewUpdateHelperDelegate)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnTouchListener(FlingPlayBackController(activity))
        setUpMiniPlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }

    private fun setUpMiniPlayer() {
        setUpPlayPauseButton()
        binding.progressIndicator.setIndicatorColor(ThemeColor.accentColor(requireContext()))
    }

    private fun setUpPlayPauseButton() {
        miniPlayerPlayPauseDrawable = PlayPauseDrawable(requireContext())
        binding.miniPlayerPlayPauseButton.setImageDrawable(miniPlayerPlayPauseDrawable)
        binding.miniPlayerPlayPauseButton.setColorFilter(
            resolveColor(
                requireActivity(),
                R.attr.iconColor,
                requireContext().secondaryTextColor(requireContext().nightMode)),
            PorterDuff.Mode.SRC_IN
        )
        binding.miniPlayerPlayPauseButton.setOnClickListener(PlayPauseButtonOnClickHandler())
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                PlayerController.currentState.collect {
                    updatePlayPauseDrawableState(
                        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    )
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                CurrentQueueState.currentSong.collect {
                    updateSongTitle()
                }
            }
        }
    }

    private fun updateSongTitle() {
        binding.miniPlayerTitle.text = MusicPlayerRemote.currentSong.title
    }

    override fun onServiceConnected() {
        updateSongTitle()
        updatePlayPauseDrawableState(false)
    }

    private fun updateProgressViews(progress: Int, total: Int) {
        binding.progressIndicator.max = total
        binding.progressIndicator.progress = progress
        binding.progressIndicator.show()
    }

    private class FlingPlayBackController(context: Context?) : View.OnTouchListener {
        var flingPlayBackController: GestureDetector =
            GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {

                        if (abs(velocityX) > abs(velocityY)) {
                            if (velocityX < 0) {
                                MusicPlayerRemote.playNextSong()
                                return true
                            } else if (velocityX > 0) {
                                MusicPlayerRemote.playPreviousSong()
                                return true
                            }
                        }
                        return false
                    }
                }
            )

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return flingPlayBackController.onTouchEvent(event)
        }
    }

    private fun updatePlayPauseDrawableState(animate: Boolean) {
        if (MusicPlayerRemote.isPlaying) {
            miniPlayerPlayPauseDrawable!!.setPause(animate)
        } else {
            miniPlayerPlayPauseDrawable!!.setPlay(animate)
        }
    }
}
