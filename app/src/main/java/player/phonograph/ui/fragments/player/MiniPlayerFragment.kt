package player.phonograph.ui.fragments.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.*
import mt.pref.ThemeColor
import mt.util.color.getSecondaryTextColor
import mt.util.color.resolveColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.databinding.FragmentMiniPlayerBinding
import player.phonograph.helper.MusicProgressViewUpdateHelper
import player.phonograph.helper.PlayPauseButtonOnClickHandler
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.views.PlayPauseDrawable
import kotlin.math.abs

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class MiniPlayerFragment : AbsMusicServiceFragment(), MusicProgressViewUpdateHelper.Callback {
    private var viewBinding: FragmentMiniPlayerBinding? = null
    private val binding get() = viewBinding!!

    private var miniPlayerPlayPauseDrawable: PlayPauseDrawable? = null
    private var progressViewUpdateHelper: MusicProgressViewUpdateHelper = MusicProgressViewUpdateHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        viewBinding = FragmentMiniPlayerBinding.inflate(layoutInflater)
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
                getSecondaryTextColor(requireContext(), !App.instance.nightMode)),
            PorterDuff.Mode.SRC_IN
        )
        binding.miniPlayerPlayPauseButton.setOnClickListener(PlayPauseButtonOnClickHandler())
    }

    private fun updateSongTitle() {
        binding.miniPlayerTitle.text = MusicPlayerRemote.currentSong.title
    }

    override fun onServiceConnected() {
        updateSongTitle()
        updatePlayPauseDrawableState(false)
    }

    override fun onPlayingMetaChanged() {
        updateSongTitle()
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState(true)
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        binding.progressIndicator.max = total
        binding.progressIndicator.progress = progress
        binding.progressIndicator.show()
    }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
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

    protected fun updatePlayPauseDrawableState(animate: Boolean) {
        if (MusicPlayerRemote.isPlaying) {
            miniPlayerPlayPauseDrawable!!.setPause(animate)
        } else {
            miniPlayerPlayPauseDrawable!!.setPlay(animate)
        }
    }
}
