
package player.phonograph.ui.fragments.player.card

import android.animation.Animator
import android.animation.AnimatorSet
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentContainerView
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import kotlin.math.max
import player.phonograph.R
import player.phonograph.adapter.base.MediaEntryViewHolder
import player.phonograph.databinding.FragmentCardPlayerBinding
import player.phonograph.model.Song
import player.phonograph.model.infoString
import player.phonograph.notification.ErrorNotification
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.fragments.player.AbsPlayerFragment
import player.phonograph.ui.fragments.player.PlayerAlbumCoverFragment
import player.phonograph.util.Util.isLandscape
import player.phonograph.util.ViewUtil
import player.phonograph.util.menu.MenuClickListener
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.Util

class CardPlayerFragment :
    AbsPlayerFragment(),
    PlayerAlbumCoverFragment.Callbacks,
    SlidingUpPanelLayout.PanelSlideListener {

    private var _viewBinding: FragmentCardPlayerBinding? = null
    private val viewBinding: FragmentCardPlayerBinding get() = _viewBinding!!

    @get:ColorInt
    override var paletteColor = 0
        private set

    private lateinit var impl: Impl

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        impl = (if (isLandscape(resources)) LandscapeImpl(this) else PortraitImpl(this))
        _viewBinding = FragmentCardPlayerBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        impl.init()

        viewBinding.playerSlidingLayout.let { slidingLayout ->
            slidingLayout.addPanelSlideListener(this)
            slidingLayout.setAntiDragView(view.findViewById(R.id.draggable_area))
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                impl.setUpPanelAndAlbumCoverHeight()
            }
        })

        // for some reason the xml attribute doesn't get applied here.
        viewBinding.playingQueueCard.setCardBackgroundColor(
            Util.resolveColor(activity, R.attr.cardBackgroundColor)
        )
    }

    override fun onDestroyView() {
        viewBinding.playerRecyclerView.itemAnimator = null
        viewBinding.playerRecyclerView.adapter = null
        viewBinding.playerRecyclerView.layoutManager = null
        super.onDestroyView()
        _viewBinding = null
    }

    override fun onPause() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager!!.cancelDrag()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        checkToggleToolbar(viewBinding.toolbarContainer)
    }

    override fun onServiceConnected() {
        updateQueue()
        updateCurrentSong()
        viewModel.updateFavoriteState(MusicPlayerRemote.currentSong)
        viewModel.loadLyrics(MusicPlayerRemote.currentSong)
    }

    override fun onPlayingMetaChanged() {
        updateCurrentSong()
        updateQueuePosition()
        viewModel.updateFavoriteState(MusicPlayerRemote.currentSong)
        viewModel.loadLyrics(MusicPlayerRemote.currentSong)
    }

    override fun onQueueChanged() {
        updateQueue()
    }

    override fun onMediaStoreChanged() {
        updateQueue()
        viewModel.updateFavoriteState(MusicPlayerRemote.currentSong)
    }

    private fun updateQueue() {
        playingQueueAdapter!!.dataset = MusicPlayerRemote.playingQueue
        playingQueueAdapter!!.current = MusicPlayerRemote.position
        viewBinding.playerQueueSubHeader.text = upNextAndQueueTime
        if (viewBinding.playerSlidingLayout.panelState == PanelState.COLLAPSED) {
            resetToCurrentPosition()
        }
    }

    private fun updateQueuePosition() {
        playingQueueAdapter!!.current = MusicPlayerRemote.position
        viewBinding.playerQueueSubHeader.text = upNextAndQueueTime
        if (viewBinding.playerSlidingLayout.panelState == PanelState.COLLAPSED) {
            resetToCurrentPosition()
        }
    }

    private fun updateCurrentSong() {
        viewModel.currentSong = MusicPlayerRemote.currentSong
        impl.onCurrentSongChanged()
    }

    override fun setUpControllerFragment() {
        playbackControlsFragment = childFragmentManager.findFragmentById(
            R.id.playback_controls_fragment
        ) as CardPlayerControllerFragment
    }

    override fun setUpCoverFragment() {
        playerAlbumCoverFragment = (
            childFragmentManager.findFragmentById(
                R.id.player_album_cover_fragment
            ) as PlayerAlbumCoverFragment
            )
            .apply { setCallbacks(this@CardPlayerFragment) }
    }

    override fun getImplToolbar(): Toolbar = viewBinding.playerToolbar

    override fun implementRecyclerView() {
        val animator: GeneralItemAnimator = RefactoredDefaultItemAnimator()
        viewBinding.playerRecyclerView.layoutManager = layoutManager
        viewBinding.playerRecyclerView.adapter = wrappedAdapter
        viewBinding.playerRecyclerView.itemAnimator = animator
        recyclerViewDragDropManager!!.attachRecyclerView(viewBinding.playerRecyclerView)
        layoutManager!!.scrollToPositionWithOffset(MusicPlayerRemote.position + 1, 0)
    }

    private fun animateColorChange(newColor: Int) {
        impl.animateColorChange(newColor)
        paletteColor = newColor
    }

    override fun onShow() {
        playbackControlsFragment.show()
    }

    override fun onHide() {
        playbackControlsFragment.hide()
        onBackPressed()
    }

    override fun onBackPressed(): Boolean {
        val wasExpanded = viewBinding.playerSlidingLayout.panelState == PanelState.EXPANDED
        viewBinding.playerSlidingLayout.panelState = PanelState.COLLAPSED
        return wasExpanded
    }

    override fun onColorChanged(color: Int) {
        animateColorChange(color)
        playbackControlsFragment.setDark(ColorUtil.isColorLight(color))
        callbacks!!.onPaletteColorChanged()
    }

    override fun onToolbarToggled() {
        toggleToolbar(viewBinding.toolbarContainer)
    }

    override fun onPanelSlide(view: View, slide: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val density = resources.displayMetrics.density
            val cardElevation = (6 * slide + 2) * density
            if (!isValidElevation(cardElevation)) return // we have received some crash reports in setCardElevation()
            viewBinding.playingQueueCard.cardElevation = cardElevation
            val buttonElevation = (2 * Math.max(0f, 1 - slide * 16) + 2) * density
            if (!isValidElevation(buttonElevation)) return
            (playbackControlsFragment as CardPlayerControllerFragment).playerPlayPauseFab.elevation = buttonElevation
        }
    }

    private fun isValidElevation(elevation: Float): Boolean {
        return elevation >= -Float.MAX_VALUE && elevation <= Float.MAX_VALUE
    }

    override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
        when (newState) {
            PanelState.COLLAPSED -> onPanelCollapsed(panel)
            PanelState.ANCHORED ->
                viewBinding.playerSlidingLayout.panelState =
                    PanelState.COLLAPSED // this fixes a bug where the panel would get stuck for some reason
            else -> Unit
        }
    }

    private fun onPanelCollapsed(panel: View) {
        resetToCurrentPosition()
    }

    private fun resetToCurrentPosition() {
        viewBinding.playerRecyclerView.stopScroll()
        layoutManager!!.scrollToPositionWithOffset(MusicPlayerRemote.position + 1, 0)
    }

    private abstract class BaseImpl(protected var fragment: CardPlayerFragment) : Impl {

        fun createDefaultColorChangeAnimatorSet(newColor: Int): AnimatorSet {
            val fab = (fragment.playbackControlsFragment as CardPlayerControllerFragment).playerPlayPauseFab
            val progressSliderHeight = (fragment.playbackControlsFragment as CardPlayerControllerFragment).progressSliderHeight
            if (progressSliderHeight < 0) {
                ErrorNotification.init()
                ErrorNotification.postErrorNotification(
                    IllegalStateException(
                        "CardPlayer's progressSliderHeight is less than 0: $progressSliderHeight"
                    ).also { it.stackTrace = Thread.currentThread().stackTrace },
                    "UI ERROR"
                )
            }

            val backgroundAnimator: Animator =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val x = fab.x + fab.width / 2 + fragment.playbackControlsFragment.requireView().x
                    val y = fab.y + fab.height / 2 + fragment.playbackControlsFragment.requireView().y + progressSliderHeight
                    val startRadius = max(fab.width / 2, fab.height / 2)
                    val endRadius = max(
                        fragment.viewBinding.colorBackground.width,
                        fragment.viewBinding.colorBackground.height
                    )
                    fragment.viewBinding.colorBackground.setBackgroundColor(newColor)
                    ViewAnimationUtils.createCircularReveal(
                        fragment.viewBinding.colorBackground,
                        x.toInt(),
                        y.toInt(),
                        startRadius.toFloat(),
                        endRadius.toFloat()
                    )
                } else {
                    ViewUtil.createBackgroundColorTransition(
                        fragment.viewBinding.colorBackground,
                        fragment.paletteColor,
                        newColor
                    )
                }

            val animatorSet =
                AnimatorSet()
                    .apply {
                        play(backgroundAnimator)
                        if (!ViewUtil.isWindowBackgroundDarkSafe(fragment.activity)) {
                            play(
                                ViewUtil.createTextColorTransition(
                                    fragment.viewBinding.playerQueueSubHeader,
                                    if (ColorUtil.isColorLight(fragment.paletteColor)) ColorUtil.darkenColor(
                                        fragment.paletteColor
                                    ) else fragment.paletteColor,
                                    if (ColorUtil.isColorLight(newColor)) ColorUtil.darkenColor(
                                        newColor
                                    ) else newColor
                                )
                            )
                        }
                        duration = ViewUtil.PHONOGRAPH_ANIM_TIME.toLong()
                    }
            return animatorSet
        }

        override fun animateColorChange(newColor: Int) {
            if (ViewUtil.isWindowBackgroundDarkSafe(fragment.activity)) {
                fragment.viewBinding.playerQueueSubHeader.setTextColor(
                    ThemeColor.textColorSecondary(fragment.requireActivity())
                )
            }
        }
    }

    private class PortraitImpl(fragment: CardPlayerFragment) : BaseImpl(fragment) {
        var currentSongViewHolder: MediaEntryViewHolder? = null
        override fun init() {
            currentSongViewHolder = MediaEntryViewHolder(
                fragment.requireView().findViewById(R.id.current_song)
            )
            currentSongViewHolder!!.separator!!.visibility = View.VISIBLE
            currentSongViewHolder!!.shortSeparator!!.visibility = View.GONE
            currentSongViewHolder!!.image!!.scaleType = ImageView.ScaleType.CENTER
            currentSongViewHolder!!.image!!.setColorFilter(
                Util.resolveColor(
                    fragment.activity,
                    R.attr.iconColor,
                    ThemeColor.textColorSecondary(
                        fragment.requireActivity()
                    )
                ),
                PorterDuff.Mode.SRC_IN
            )
            currentSongViewHolder!!.image!!.setImageResource(R.drawable.ic_volume_up_white_24dp)
            currentSongViewHolder!!.itemView.setOnClickListener {
                // toggle the panel
                if (fragment.viewBinding.playerSlidingLayout.panelState == PanelState.COLLAPSED) {
                    fragment.viewBinding.playerSlidingLayout.panelState = PanelState.EXPANDED
                } else if (fragment.viewBinding.playerSlidingLayout.panelState == PanelState.EXPANDED) {
                    fragment.viewBinding.playerSlidingLayout.panelState = PanelState.COLLAPSED
                }
            }
            currentSongViewHolder!!.menu!!.setOnClickListener(object :
                    MenuClickListener(
                        (fragment.activity as AppCompatActivity),
                        R.menu.menu_item_playing_queue_song
                    ) {
                    override val song: Song = MusicPlayerRemote.currentSong

                    override fun onMenuItemClick(item: MenuItem): Boolean {
                        when (item.itemId) {
                            R.id.action_remove_from_playing_queue -> {
                                MusicPlayerRemote.removeFromQueue(MusicPlayerRemote.position)
                                return true
                            }
                        }
                        return fragment.onMenuItemClick(item)
                    }
                })
        }

        override fun setUpPanelAndAlbumCoverHeight() {
            val albumCoverContainer: FragmentContainerView = fragment.requireView().findViewById(
                R.id.player_album_cover_fragment
            )
            val availablePanelHeight =
                fragment.viewBinding.playerSlidingLayout.height - fragment.requireView().findViewById<View>(
                    R.id.player_content
                ).height + ViewUtil.convertDpToPixel(
                    8f,
                    fragment.resources
                )
                    .toInt()
            val minPanelHeight = ViewUtil.convertDpToPixel((72 + 24).toFloat(), fragment.resources).toInt()
            if (availablePanelHeight < minPanelHeight) {
                albumCoverContainer.layoutParams.height = albumCoverContainer.height - (minPanelHeight - availablePanelHeight)
                // albumCoverContainer.forceSquare(false)
            }
            fragment.viewBinding.playerSlidingLayout.panelHeight = Math.max(
                minPanelHeight,
                availablePanelHeight
            )
            (fragment.activity as AbsSlidingMusicPanelActivity?)!!.setAntiDragView(
                fragment.viewBinding.playerSlidingLayout.findViewById(R.id.player_panel)
            )
        }

        override fun onCurrentSongChanged() {
            currentSongViewHolder!!.title!!.text = fragment.viewModel.currentSong.title
            currentSongViewHolder!!.text!!.text =
                fragment.viewModel.currentSong.infoString()
        }

        override fun animateColorChange(newColor: Int) {
            super.animateColorChange(newColor)
            fragment.viewBinding.playerSlidingLayout.setBackgroundColor(fragment.paletteColor)
            createDefaultColorChangeAnimatorSet(newColor).start()
        }
    }

    private class LandscapeImpl(fragment: CardPlayerFragment) : BaseImpl(fragment) {
        override fun init() {}
        override fun setUpPanelAndAlbumCoverHeight() {
            val panelHeight = fragment.viewBinding.playerSlidingLayout.height - fragment.playbackControlsFragment.requireView()
                .height
            fragment.viewBinding.playerSlidingLayout.panelHeight = panelHeight
            (fragment.activity as AbsSlidingMusicPanelActivity?)!!.setAntiDragView(
                fragment.viewBinding.playerSlidingLayout.findViewById(R.id.player_panel)
            )
        }

        override fun onCurrentSongChanged() {
            fragment.viewBinding.playerToolbar.title = fragment.viewModel.currentSong.title
            fragment.viewBinding.playerToolbar.subtitle =
                fragment.viewModel.currentSong.infoString()
        }

        override fun animateColorChange(newColor: Int) {
            super.animateColorChange(newColor)
            fragment.viewBinding.playerSlidingLayout.setBackgroundColor(fragment.paletteColor)
            val animatorSet = createDefaultColorChangeAnimatorSet(newColor)
            animatorSet.play(
                ViewUtil.createBackgroundColorTransition(
                    fragment.viewBinding.playerToolbar,
                    fragment.paletteColor,
                    newColor
                )
            )
                .with(
                    ViewUtil.createBackgroundColorTransition(
                        fragment.requireView().findViewById(R.id.status_bar),
                        ColorUtil.darkenColor(
                            fragment.paletteColor
                        ),
                        ColorUtil.darkenColor(newColor)
                    )
                )
            animatorSet.start()
        }
    }
}
