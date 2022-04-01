
package player.phonograph.ui.fragments.player.flat

import android.animation.AnimatorSet
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.adapter.base.MediaEntryViewHolder
import player.phonograph.databinding.FragmentFlatPlayerBinding
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.helper.menu.SongMenuHelper.ClickMenuListener
import player.phonograph.model.Song
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.fragments.player.AbsPlayerFragment
import player.phonograph.ui.fragments.player.PlayerAlbumCoverFragment
import player.phonograph.util.FavoriteUtil.isFavorite
import player.phonograph.util.ImageUtil
import player.phonograph.util.MusicUtil
import player.phonograph.util.Util.isLandscape
import player.phonograph.util.ViewUtil
import player.phonograph.views.WidthFitSquareLayout
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.ToolbarColorUtil
import util.mddesign.util.Util

class FlatPlayerFragment :
    AbsPlayerFragment(),
    PlayerAlbumCoverFragment.Callbacks,
    SlidingUpPanelLayout.PanelSlideListener {

    private var _viewBinding: FragmentFlatPlayerBinding? = null
    private val viewBinding: FragmentFlatPlayerBinding get() = _viewBinding!!

    @get:ColorInt
    override var paletteColor = 0
        private set

    private lateinit var impl: Impl

    private lateinit var playbackControlsFragment: FlatPlayerPlaybackControlsFragment // setUpSubFragments()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _viewBinding = FragmentFlatPlayerBinding.inflate(inflater)
        impl = (if (isLandscape(resources)) LandscapeImpl(this) else PortraitImpl(this))
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        impl.init()
        setUpSubFragments()

        viewBinding.playerSlidingLayout?.let { slidingLayout ->
            slidingLayout.addPanelSlideListener(this)
            slidingLayout.setAntiDragView(view.findViewById(R.id.draggable_area))
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                impl.setUpPanelAndAlbumCoverHeight()
            }
        })
    }

    override fun onDestroyView() {
        if (viewBinding.playerSlidingLayout != null) {
            viewBinding.playerSlidingLayout!!.removePanelSlideListener(this)
        }
        viewBinding.playerRecyclerView.itemAnimator = null
        viewBinding.playerRecyclerView.adapter = null
        super.onDestroyView()
    }

    override fun onPause() {
        recyclerViewDragDropManager?.cancelDrag()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        checkToggleToolbar(viewBinding.toolbarContainer)
    }

    override fun onServiceConnected() {
        updateQueue()
        updateCurrentSong()
        updateFavoriteState(MusicPlayerRemote.getCurrentSong())
        viewModel.unlockLyrics()
        loadAndRefreshLyrics(MusicPlayerRemote.getCurrentSong())
    }

    override fun onPlayingMetaChanged() {
        updateCurrentSong()
        updateFavoriteState(MusicPlayerRemote.getCurrentSong())
        updateQueuePosition()
        viewModel.unlockLyrics()
        loadAndRefreshLyrics(MusicPlayerRemote.getCurrentSong())
    }

    override fun onQueueChanged() {
        updateQueue()
    }

    override fun onMediaStoreChanged() {
        updateQueue()
        updateFavoriteState(MusicPlayerRemote.getCurrentSong())
    }

    override fun onPlayStateChanged() {
        loadAndRefreshLyrics(MusicPlayerRemote.getCurrentSong())
    }

    private fun updateQueue() {
        playingQueueAdapter!!.swapDataSet(MusicPlayerRemote.getPlayingQueue(), MusicPlayerRemote.getPosition())
        viewBinding.playerQueueSubHeader.text = upNextAndQueueTime
        if (viewBinding.playerSlidingLayout == null || viewBinding.playerSlidingLayout!!.panelState == PanelState.COLLAPSED) {
            resetToCurrentPosition()
        }
    }

    private fun updateQueuePosition() {
        playingQueueAdapter!!.setCurrent(MusicPlayerRemote.getPosition())
        viewBinding.playerQueueSubHeader.text = upNextAndQueueTime
        if (viewBinding.playerSlidingLayout == null || viewBinding.playerSlidingLayout!!.panelState == PanelState.COLLAPSED) {
            resetToCurrentPosition()
        }
    }

    private fun updateCurrentSong() {
        impl.updateCurrentSong(MusicPlayerRemote.getCurrentSong())
    }

    private fun setUpSubFragments() {
        playbackControlsFragment = childFragmentManager.findFragmentById(R.id.playback_controls_fragment) as FlatPlayerPlaybackControlsFragment
        playerAlbumCoverFragment = (childFragmentManager.findFragmentById(R.id.player_album_cover_fragment) as PlayerAlbumCoverFragment)
            .apply { setCallbacks(this@FlatPlayerFragment) }
    }

    override fun getImplToolbar(): Toolbar = viewBinding.playerToolbar

    override fun implementRecyclerView() {
        val animator: GeneralItemAnimator = RefactoredDefaultItemAnimator()
        viewBinding.playerRecyclerView.layoutManager = layoutManager
        viewBinding.playerRecyclerView.adapter = wrappedAdapter
        viewBinding.playerRecyclerView.itemAnimator = animator
        recyclerViewDragDropManager!!.attachRecyclerView(viewBinding.playerRecyclerView)
        layoutManager!!.scrollToPositionWithOffset(MusicPlayerRemote.getPosition() + 1, 0)
    }

    override fun updateFavoriteIcon(isFavorite: Boolean) {
        val res = if (isFavorite) R.drawable.ic_favorite_white_24dp else R.drawable.ic_favorite_border_white_24dp
        val color = ToolbarColorUtil.toolbarContentColor(requireActivity(), Color.TRANSPARENT)
        val drawable = ImageUtil.getTintedVectorDrawable(requireActivity(), res, color)
        viewBinding.playerToolbar.menu
            .findItem(R.id.action_toggle_favorite)
            .setIcon(drawable)
            .title = if (isFavorite) getString(R.string.action_remove_from_favorites) else getString(R.string.action_add_to_favorites)
    }

    override fun hideLyricsMenuItem() {
        viewBinding.playerToolbar.menu.removeItem(R.id.action_show_lyrics)
    }

    override fun showLyricsMenuItem() {
        activity?.let { activity ->
            if (viewBinding.playerToolbar.menu.findItem(R.id.action_show_lyrics) == null) {
                viewBinding.playerToolbar.menu
                    .add(Menu.NONE, R.id.action_show_lyrics, Menu.NONE, R.string.action_show_lyrics)
                    .setIcon(ImageUtil.getTintedVectorDrawable(activity, R.drawable.ic_comment_text_outline_white_24dp, ToolbarColorUtil.toolbarContentColor(activity, Color.TRANSPARENT)))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
        }
    }

    private fun animateColorChange(newColor: Int) {
        impl.animateColorChange(newColor)
        paletteColor = newColor
    }

    override fun toggleFavorite(song: Song) {
        super.toggleFavorite(song)
        if (song.id == MusicPlayerRemote.getCurrentSong().id) {
            if (isFavorite(requireActivity(), song)) {
                playerAlbumCoverFragment.showHeartAnimation()
            }
            updateFavoriteState(song)
        }
    }

    override fun onShow() {
        playbackControlsFragment.show()
    }

    override fun onHide() {
        playbackControlsFragment.hide()
        onBackPressed()
    }

    override fun onBackPressed(): Boolean {
        var wasExpanded = false
        if (viewBinding.playerSlidingLayout != null) {
            wasExpanded = viewBinding.playerSlidingLayout!!.panelState == PanelState.EXPANDED
            viewBinding.playerSlidingLayout!!.panelState = PanelState.COLLAPSED
        }
        return wasExpanded
    }

    override fun onColorChanged(color: Int) {
        animateColorChange(color)
        playbackControlsFragment.setDark(ColorUtil.isColorLight(color))
        callbacks!!.onPaletteColorChanged()
    }

    override fun onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.getCurrentSong())
    }

    override fun onToolbarToggled() {
        toggleToolbar(viewBinding.toolbarContainer)
    }

    override fun onPanelSlide(view: View, slide: Float) {}
    override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
        when (newState) {
            PanelState.COLLAPSED -> onPanelCollapsed(panel)
            PanelState.ANCHORED ->
                viewBinding.playerSlidingLayout!!.panelState =
                    PanelState.COLLAPSED // this fixes a bug where the panel would get stuck for some reason
            else -> Unit
        }
    }

    private fun onPanelCollapsed(panel: View?) {
        resetToCurrentPosition()
    }

    private fun resetToCurrentPosition() {
        viewBinding.playerRecyclerView.stopScroll()
        layoutManager!!.scrollToPositionWithOffset(MusicPlayerRemote.getPosition() + 1, 0)
    }

    private abstract class BaseImpl(protected var fragment: FlatPlayerFragment) : Impl {
        fun createDefaultColorChangeAnimatorSet(newColor: Int): AnimatorSet {
            val backgroundAnimator =
                ViewUtil.createBackgroundColorTransition(fragment.playbackControlsFragment.view, fragment.paletteColor, newColor)
            val statusBarAnimator =
                ViewUtil.createBackgroundColorTransition(fragment.viewBinding.playerStatusBar, fragment.paletteColor, newColor)
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(backgroundAnimator, statusBarAnimator)
            if (!Util.isWindowBackgroundDark(fragment.activity)) {
                val adjustedLastColor =
                    if (ColorUtil.isColorLight(fragment.paletteColor)) ColorUtil.darkenColor(fragment.paletteColor) else fragment.paletteColor
                val adjustedNewColor = if (ColorUtil.isColorLight(newColor)) ColorUtil.darkenColor(newColor) else newColor
                val subHeaderAnimator =
                    ViewUtil.createTextColorTransition(fragment.viewBinding.playerQueueSubHeader, adjustedLastColor, adjustedNewColor)
                animatorSet.play(subHeaderAnimator)
            }
            animatorSet.duration = ViewUtil.PHONOGRAPH_ANIM_TIME.toLong()
            return animatorSet
        }

        override fun animateColorChange(newColor: Int) {
            if (Util.isWindowBackgroundDark(fragment.activity)) {
                fragment.viewBinding.playerQueueSubHeader.setTextColor(ThemeColor.textColorSecondary(fragment.requireActivity()))
            }
        }
    }

    private class PortraitImpl(fragment: FlatPlayerFragment) : BaseImpl(fragment) {
        var currentSongViewHolder: MediaEntryViewHolder? = null
        var song: Song = Song.EMPTY_SONG
        override fun init() {
            currentSongViewHolder = MediaEntryViewHolder(fragment.requireView().findViewById(R.id.current_song))
            currentSongViewHolder!!.separator!!.visibility = View.VISIBLE
            currentSongViewHolder!!.shortSeparator!!.visibility = View.GONE
            currentSongViewHolder!!.image!!.scaleType = ImageView.ScaleType.CENTER
            currentSongViewHolder!!.image!!.setColorFilter(
                Util.resolveColor(
                    fragment.activity, R.attr.iconColor,
                    ThemeColor.textColorSecondary(
                        fragment.requireActivity()
                    )
                ),
                PorterDuff.Mode.SRC_IN
            )
            currentSongViewHolder!!.image!!.setImageResource(R.drawable.ic_volume_up_white_24dp)
            currentSongViewHolder!!.itemView.setOnClickListener {
                // toggle the panel
                if (fragment.viewBinding.playerSlidingLayout!!.panelState == PanelState.COLLAPSED) {
                    fragment.viewBinding.playerSlidingLayout!!.panelState = PanelState.EXPANDED
                } else if (fragment.viewBinding.playerSlidingLayout!!.panelState == PanelState.EXPANDED) {
                    fragment.viewBinding.playerSlidingLayout!!.panelState = PanelState.COLLAPSED
                }
            }
            currentSongViewHolder!!.menu!!.setOnClickListener(

                object : ClickMenuListener(fragment.requireActivity() as AppCompatActivity, R.menu.menu_item_playing_queue_song) {

                    override val song: Song = MusicPlayerRemote.getCurrentSong()

                    override fun onMenuItemClick(item: MenuItem): Boolean {
                        when (item.itemId) {
                            R.id.action_remove_from_playing_queue -> {
                                MusicPlayerRemote.removeFromQueue(MusicPlayerRemote.getPosition())
                                return true
                            }
                        }
                        return fragment.onMenuItemClick(item)
                    }
                })
        }

        override fun setUpPanelAndAlbumCoverHeight() {
            val albumCoverContainer: WidthFitSquareLayout = fragment.requireView().findViewById(R.id.album_cover_container)
            val availablePanelHeight =
                fragment.viewBinding.playerSlidingLayout!!.height - fragment.requireView().findViewById<View>(R.id.player_content).height
            val minPanelHeight = ViewUtil.convertDpToPixel((8 + 72 + 24).toFloat(), fragment.resources)
                .toInt() + fragment.resources.getDimensionPixelSize(R.dimen.progress_container_height) + fragment.resources.getDimensionPixelSize(
                R.dimen.media_controller_container_height
            )
            if (availablePanelHeight < minPanelHeight) {
                albumCoverContainer.layoutParams.height = albumCoverContainer.height - (minPanelHeight - availablePanelHeight)
                albumCoverContainer.forceSquare(false)
            }
            fragment.viewBinding.playerSlidingLayout!!.panelHeight = Math.max(minPanelHeight, availablePanelHeight)
            (fragment.activity as AbsSlidingMusicPanelActivity?)!!.setAntiDragView(
                fragment.viewBinding.playerSlidingLayout!!.findViewById(
                    R.id.player_panel
                )
            )
        }

        override fun updateCurrentSong(song: Song) {
            this.song = song
            currentSongViewHolder!!.title!!.text = song.title
            currentSongViewHolder!!.text!!.text = MusicUtil.getSongInfoString(song)
        }

        override fun animateColorChange(newColor: Int) {
            super.animateColorChange(newColor)
            createDefaultColorChangeAnimatorSet(newColor).start()
        }
    }

    private class LandscapeImpl(fragment: FlatPlayerFragment) : BaseImpl(fragment) {
        override fun init() {}
        override fun setUpPanelAndAlbumCoverHeight() {
            (fragment.activity as AbsSlidingMusicPanelActivity?)!!.setAntiDragView(fragment.requireView().findViewById(R.id.player_panel))
        }

        override fun updateCurrentSong(song: Song) {
            fragment.viewBinding.playerToolbar.title = song.title
            fragment.viewBinding.playerToolbar.subtitle = MusicUtil.getSongInfoString(song)
        }

        override fun animateColorChange(newColor: Int) {
            super.animateColorChange(newColor)
            val animatorSet = createDefaultColorChangeAnimatorSet(newColor)
            animatorSet.play(
                ViewUtil.createBackgroundColorTransition(
                    fragment.viewBinding.playerToolbar,
                    fragment.paletteColor,
                    newColor
                )
            )
            animatorSet.start()
        }
    }
}
