
package player.phonograph.ui.fragments.player.card

import android.animation.Animator
import android.animation.AnimatorSet
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState
import player.phonograph.R
import player.phonograph.adapter.base.MediaEntryViewHolder
import player.phonograph.adapter.song.PlayingQueueAdapter
import player.phonograph.databinding.FragmentCardPlayerBinding
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

    private lateinit var playbackControlsFragment: CardPlayerPlaybackControlsFragment // setUpSubFragments()
    private var layoutManager: LinearLayoutManager? = null
    private var playingQueueAdapter: PlayingQueueAdapter? = null
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        impl = (if (isLandscape(resources)) LandscapeImpl(this) else PortraitImpl(this))
        _viewBinding = FragmentCardPlayerBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        impl.init()
        setUpPlayerToolbar()
        setUpSubFragments()
        setUpRecyclerView()

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
        viewBinding.playingQueueCard.setCardBackgroundColor(Util.resolveColor(activity, R.attr.cardBackgroundColor))
    }

    override fun onDestroyView() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager!!.release()
            recyclerViewDragDropManager = null
        }
        viewBinding.playerRecyclerView.itemAnimator = null
        viewBinding.playerRecyclerView.adapter = null
        if (wrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(wrappedAdapter)
            wrappedAdapter = null
        }
        playingQueueAdapter = null
        layoutManager = null
        super.onDestroyView()
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
        updateFavoriteState(MusicPlayerRemote.getCurrentSong())
        loadAndRefreshLyrics(MusicPlayerRemote.getCurrentSong())
    }

    override fun onPlayingMetaChanged() {
        updateCurrentSong()
        updateFavoriteState(MusicPlayerRemote.getCurrentSong())
        updateQueuePosition()
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
        if (viewBinding.playerSlidingLayout.panelState == PanelState.COLLAPSED) {
            resetToCurrentPosition()
        }
    }

    private fun updateQueuePosition() {
        playingQueueAdapter!!.setCurrent(MusicPlayerRemote.getPosition())
        viewBinding.playerQueueSubHeader.text = upNextAndQueueTime
        if (viewBinding.playerSlidingLayout.panelState == PanelState.COLLAPSED) {
            resetToCurrentPosition()
        }
    }

    private fun updateCurrentSong() {
        impl.updateCurrentSong(MusicPlayerRemote.getCurrentSong())
    }

    private fun setUpSubFragments() {
        playbackControlsFragment = childFragmentManager.findFragmentById(R.id.playback_controls_fragment) as CardPlayerPlaybackControlsFragment
        playerAlbumCoverFragment = (childFragmentManager.findFragmentById(R.id.player_album_cover_fragment) as PlayerAlbumCoverFragment)
            .apply { setCallbacks(this@CardPlayerFragment) }
    }

    private fun setUpPlayerToolbar() {
        viewBinding.playerToolbar.inflateMenu(R.menu.menu_player)
        viewBinding.playerToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
        viewBinding.playerToolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        viewBinding.playerToolbar.setOnMenuItemClickListener(this)
    }

    private fun setUpRecyclerView() {
        recyclerViewDragDropManager = RecyclerViewDragDropManager()
        val animator: GeneralItemAnimator = RefactoredDefaultItemAnimator()
        playingQueueAdapter = PlayingQueueAdapter(
            (requireActivity() as AppCompatActivity),
            MusicPlayerRemote.getPlayingQueue(),
            MusicPlayerRemote.getPosition(),
            R.layout.item_list,
            false,
            null
        )
        wrappedAdapter = recyclerViewDragDropManager!!.createWrappedAdapter(playingQueueAdapter!!)
        layoutManager = LinearLayoutManager(activity)
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
        val wasExpanded = viewBinding.playerSlidingLayout.panelState == PanelState.EXPANDED
        viewBinding.playerSlidingLayout.panelState = PanelState.COLLAPSED
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

    override fun onPanelSlide(view: View, slide: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val density = resources.displayMetrics.density
            val cardElevation = (6 * slide + 2) * density
            if (!isValidElevation(cardElevation)) return // we have received some crash reports in setCardElevation()
            viewBinding.playingQueueCard.cardElevation = cardElevation
            val buttonElevation = (2 * Math.max(0f, 1 - slide * 16) + 2) * density
            if (!isValidElevation(buttonElevation)) return
            playbackControlsFragment.viewBinding.playerPlayPauseFab.elevation = buttonElevation
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
        layoutManager!!.scrollToPositionWithOffset(MusicPlayerRemote.getPosition() + 1, 0)
    }

    private abstract class BaseImpl(protected var fragment: CardPlayerFragment) : Impl {
        fun createDefaultColorChangeAnimatorSet(newColor: Int): AnimatorSet {
            val backgroundAnimator: Animator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val x =
                    (
                        fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.x + fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.width / 2 + fragment.playbackControlsFragment.requireView()
                            .x
                        ).toInt()
                val y =
                    (
                        fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.y + fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.height / 2 + fragment.playbackControlsFragment.requireView()
                            .y + fragment.playbackControlsFragment.viewBinding.playerProgressSlider.height
                        ).toInt()
                val startRadius = Math.max(
                    fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.width / 2,
                    fragment.playbackControlsFragment.viewBinding.playerPlayPauseFab.height / 2
                ).toFloat()
                val endRadius =
                    Math.max(fragment.viewBinding.colorBackground.width, fragment.viewBinding.colorBackground.height).toFloat()
                fragment.viewBinding.colorBackground.setBackgroundColor(newColor)
                ViewAnimationUtils.createCircularReveal(fragment.viewBinding.colorBackground, x, y, startRadius, endRadius)
            } else {
                ViewUtil.createBackgroundColorTransition(fragment.viewBinding.colorBackground, fragment.paletteColor, newColor)
            }
            val animatorSet = AnimatorSet()
            animatorSet.play(backgroundAnimator)
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

    private class PortraitImpl(fragment: CardPlayerFragment) : BaseImpl(fragment) {
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
                if (fragment.viewBinding.playerSlidingLayout.panelState == PanelState.COLLAPSED) {
                    fragment.viewBinding.playerSlidingLayout.panelState = PanelState.EXPANDED
                } else if (fragment.viewBinding.playerSlidingLayout.panelState == PanelState.EXPANDED) {
                    fragment.viewBinding.playerSlidingLayout.panelState = PanelState.COLLAPSED
                }
            }
            currentSongViewHolder!!.menu!!.setOnClickListener(object :
                    ClickMenuListener((fragment.activity as AppCompatActivity), R.menu.menu_item_playing_queue_song) {
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
                fragment.viewBinding.playerSlidingLayout.height - fragment.requireView().findViewById<View>(R.id.player_content).height + ViewUtil.convertDpToPixel(
                    8f,
                    fragment.resources
                )
                    .toInt()
            val minPanelHeight = ViewUtil.convertDpToPixel((72 + 24).toFloat(), fragment.resources).toInt()
            if (availablePanelHeight < minPanelHeight) {
                albumCoverContainer.layoutParams.height = albumCoverContainer.height - (minPanelHeight - availablePanelHeight)
                albumCoverContainer.forceSquare(false)
            }
            fragment.viewBinding.playerSlidingLayout.panelHeight = Math.max(minPanelHeight, availablePanelHeight)
            (fragment.activity as AbsSlidingMusicPanelActivity?)!!.setAntiDragView(fragment.viewBinding.playerSlidingLayout.findViewById(R.id.player_panel))
        }

        override fun updateCurrentSong(song: Song) {
            this.song = song
            currentSongViewHolder!!.title!!.text = song.title
            currentSongViewHolder!!.text!!.text = MusicUtil.getSongInfoString(song)
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
            (fragment.activity as AbsSlidingMusicPanelActivity?)!!.setAntiDragView(fragment.viewBinding.playerSlidingLayout.findViewById(R.id.player_panel))
        }

        override fun updateCurrentSong(song: Song) {
            fragment.viewBinding.playerToolbar.title = song.title
            fragment.viewBinding.playerToolbar.subtitle = MusicUtil.getSongInfoString(song)
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
