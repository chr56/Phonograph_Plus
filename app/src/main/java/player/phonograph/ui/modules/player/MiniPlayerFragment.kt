package player.phonograph.ui.modules.player

import player.phonograph.R
import player.phonograph.databinding.FragmentMiniPlayerBinding
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.modules.panel.AbsMusicServiceFragment
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.ui.theme.ThemeSettingsDelegate.accentColor
import player.phonograph.ui.theme.getTintedDrawable
import player.phonograph.ui.theme.themeIconColor
import player.phonograph.ui.util.observe
import player.phonograph.ui.views.PlayPauseDrawable
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
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

    private val panelViewModel: PanelViewModel by viewModels(ownerProducer = { requireActivity() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentMiniPlayerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        miniPlayerPlayPauseDrawable = PlayPauseDrawable()
        observe(MusicPlayerRemote.currentState) {
            refreshPlayPauseDrawableState()
        }
        observe(queueViewModel.currentSong) { song ->
            replaceText(song?.title ?: getString(R.string.msg_empty))
        }
        binding.progressIndicator.setIndicatorColor(accentColor())
        binding.miniPlayerActionButton.setOnClickListener(PlayPauseButtonOnClickHandler())
        binding.root.setOnClickListener {
            lifecycleScope.launch { panelViewModel.requestToExpand() }
        }
        @SuppressLint("ClickableViewAccessibility")
        binding.root.setOnTouchListener(FlingPlayBackController(activity))
        lifecycle.addObserver(musicProgressUpdateDelegate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }

    override fun onServiceConnected() {
        val context = requireContext()
        val currentSong: Song? = MusicPlayerRemote.currentSong
        replaceText(currentSong?.title ?: context.getString(R.string.msg_not_available))
        replaceDrawable(miniPlayerPlayPauseDrawable)
        refreshPlayPauseDrawableState()
    }

    override fun onServiceDisconnected() {
        val context = requireContext()
        replaceText(context.getString(R.string.tips_service_disconnected))
        replaceDrawable(getTintedDrawable(R.drawable.ic_refresh_white_24dp, themeIconColor(context)))
    }

    private fun onUpdateProgress(progress: Int, total: Int) {
        binding.progressIndicator.max = total
        binding.progressIndicator.progress = progress
        binding.progressIndicator.show()
    }

    // implementation for fling to switch
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

    private fun refreshPlayPauseDrawableState() {
        val withAnimated = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        miniPlayerPlayPauseDrawable?.update(!MusicPlayerRemote.isPlaying, withAnimated)
    }
}
