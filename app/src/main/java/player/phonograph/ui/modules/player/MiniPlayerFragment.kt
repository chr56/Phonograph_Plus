package player.phonograph.ui.modules.player

import player.phonograph.R
import player.phonograph.databinding.FragmentMiniPlayerBinding
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.modules.panel.AbsMusicServiceFragment
import player.phonograph.ui.views.PlayPauseDrawable
import player.phonograph.util.component.MusicProgressUpdateDelegate
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.themeIconColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
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

    private val musicProgressUpdateDelegate = MusicProgressUpdateDelegate(::onUpdateProgress)

    override fun onCreate(savedInstanceState: Bundle?) {
        viewBinding = FragmentMiniPlayerBinding.inflate(layoutInflater)
        lifecycle.addObserver(musicProgressUpdateDelegate)
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
        binding.miniPlayerActionButton.setOnClickListener(PlayPauseButtonOnClickHandler())
        binding.progressIndicator.setIndicatorColor(accentColor())
    }

    private fun setUpPlayPauseButton() {
        miniPlayerPlayPauseDrawable = PlayPauseDrawable(requireContext())
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                MusicPlayerRemote.currentState.collect {
                    updatePlayPauseDrawableState(
                        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    )
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                queueViewModel.currentSong.collect {
                    replaceText(if (it != null) it.title else getString(R.string.empty))
                }
            }
        }
    }

    override fun onServiceConnected() {
        val context = requireContext()
        val currentSong: Song? = MusicPlayerRemote.currentSong
        replaceText(currentSong?.title ?: context.getString(R.string.empty))
        replaceDrawable(miniPlayerPlayPauseDrawable)
        updatePlayPauseDrawableState(false)
    }

    override fun onServiceDisconnected() {
        val context = requireContext()
        replaceText(context.getString(R.string.service_disconnected))
        replaceDrawable(context.getTintedDrawable(R.drawable.ic_refresh_white_24dp, themeIconColor(context)))
    }

    private fun onUpdateProgress(progress: Int, total: Int) {
        binding.progressIndicator.max = total
        binding.progressIndicator.progress = progress
        binding.progressIndicator.show()
    }

    private class FlingPlayBackController(context: Context?) : View.OnTouchListener {
        var flingPlayBackController: GestureDetector =
            GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float,
                    ): Boolean {

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

    private fun replaceText(text: String) {
        binding.miniPlayerTitle.text = text
    }

    private fun replaceDrawable(drawable: Drawable?) {
        binding.miniPlayerActionButton.setImageDrawable(drawable)
    }

    private fun updatePlayPauseDrawableState(animate: Boolean) {
        miniPlayerPlayPauseDrawable?.update(!MusicPlayerRemote.isPlaying, animate)
    }
}
