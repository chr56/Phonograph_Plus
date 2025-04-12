/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.player

import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import player.phonograph.R
import player.phonograph.databinding.FragmentQueueBinding
import player.phonograph.mechanism.actions.ActionMenuProviders
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Song
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.modules.panel.AbsMusicServiceFragment
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.util.text.buildInfoString
import player.phonograph.util.text.infoString
import player.phonograph.util.text.readableDuration
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.theme.themeIconColor
import player.phonograph.util.ui.PHONOGRAPH_ANIM_TIME
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import player.phonograph.util.ui.textColorTransitionAnimator
import util.theme.color.darkenColor
import util.theme.color.lightenColor
import util.theme.color.secondaryTextColor
import util.theme.materials.MaterialColor
import androidx.annotation.ColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withCreated
import androidx.lifecycle.withStarted
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.animation.Animator
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlin.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerQueueFragment : AbsMusicServiceFragment() {

    private var _viewBinding: FragmentQueueBinding? = null
    private val binding: FragmentQueueBinding get() = _viewBinding!!

    private val panelViewModel: PanelViewModel by viewModel(ownerProducer = { requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _viewBinding = FragmentQueueBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initCurrentSong()
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _recyclerViewDragDropManager?.let {
            recyclerViewDragDropManager.release()
            _recyclerViewDragDropManager = null
        }
        _wrappedAdapter?.let {
            WrapperAdapterUtils.releaseAll(wrappedAdapter)
            _wrappedAdapter = null
        }

        _playingQueueAdapter = null
        _layoutManager = null

        binding.playerRecyclerView.itemAnimator = null
        binding.playerRecyclerView.adapter = null
        binding.playerRecyclerView.layoutManager = null

        _viewBinding = null
    }

    override fun onPause() {
        recyclerViewDragDropManager.cancelDrag()
        super.onPause()
    }

    fun onShow() {}

    fun onHide() {}

    private lateinit var listener: MediaStoreListener
    override fun onCreate(savedInstanceState: Bundle?) {
        listener = MediaStoreListener()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(listener)
    }

    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() {
            lifecycleScope.launch(Dispatchers.Main) {
                withStarted {
                    playingQueueAdapter.dataset = MusicPlayerRemote.playingQueue
                    playingQueueAdapter.current = MusicPlayerRemote.position
                    updateQueueTime(MusicPlayerRemote.position)
                    resetToCurrentPosition(false)
                }
            }
        }
    }

    //region RecyclerView
    private var _layoutManager: LinearLayoutManager? = null
    val layoutManager: LinearLayoutManager get() = _layoutManager!!

    private var _playingQueueAdapter: PlayingQueueAdapter? = null
    val playingQueueAdapter: PlayingQueueAdapter get() = _playingQueueAdapter!!

    private var _wrappedAdapter: RecyclerView.Adapter<*>? = null
    private val wrappedAdapter: RecyclerView.Adapter<*> get() = _wrappedAdapter!!

    private var _recyclerViewDragDropManager: RecyclerViewDragDropManager? = null
    private val recyclerViewDragDropManager: RecyclerViewDragDropManager get() = _recyclerViewDragDropManager!!

    private fun initRecyclerView() {

        _playingQueueAdapter = PlayingQueueAdapter(requireActivity())
        playingQueueAdapter.dataset = MusicPlayerRemote.playingQueue
        playingQueueAdapter.current = MusicPlayerRemote.position

        _layoutManager = LinearLayoutManager(requireActivity())

        _recyclerViewDragDropManager = RecyclerViewDragDropManager()
        _wrappedAdapter = recyclerViewDragDropManager.createWrappedAdapter(playingQueueAdapter)
        recyclerViewDragDropManager.setInitiateOnTouch(true)
        recyclerViewDragDropManager.setInitiateOnLongPress(false)

        binding.playerRecyclerView.setUpFastScrollRecyclerViewColor(requireContext(), MaterialColor.Grey._500.asColor)
        binding.playerRecyclerView.adapter = wrappedAdapter
        binding.playerRecyclerView.layoutManager = layoutManager
        binding.playerRecyclerView.itemAnimator = RefactoredDefaultItemAnimator()
        recyclerViewDragDropManager.attachRecyclerView(binding.playerRecyclerView)
        layoutManager.scrollToPositionWithOffset(MusicPlayerRemote.position + 1, 0)
    }

    private fun initCurrentSong() {
        with(binding.currentSong) {
            title.isSingleLine = false
            title.maxLines = 2
            text.ellipsize = TextUtils.TruncateAt.MARQUEE
            text.isSelected = true
            separator.visibility = View.VISIBLE
            shortSeparator.visibility = View.GONE

            image.scaleType = ImageView.ScaleType.CENTER
            image.setColorFilter(themeIconColor(image.context), PorterDuff.Mode.SRC_IN)
            image.setImageResource(R.drawable.ic_volume_up_white_24dp)

            root.setOnClickListener {
                val parent = parentFragment
                if (parent is UnarySlidingUpPanelProvider) parent.requestToSwitchState() // toggle the panel
            }
            menu.setOnClickListener {
                val song: Song? = MusicPlayerRemote.currentSong
                if (song != null)
                    ActionMenuProviders.SongActionMenuProvider(showPlay = false, index = MusicPlayerRemote.position)
                        .prepareMenu(it, song)
            }
        }
    }

    //endregion


    var queue: List<Song>
        get() = playingQueueAdapter.dataset
        set(value) {
            playingQueueAdapter.dataset = value
        }

    var current: Int
        get() = playingQueueAdapter.current
        set(value) {
            playingQueueAdapter.current = value
        }

    fun scrollToQueueItem(position: Int) = layoutManager.scrollToPositionWithOffset(position, 0)
    fun stopScroll() = binding.playerRecyclerView.stopScroll()

    var positionLockState: Boolean = false

    fun resetToCurrentPosition(force: Boolean) {
        if (!positionLockState || force) {
            stopScroll()
            scrollToQueueItem(MusicPlayerRemote.position + 1)
        }
    }

    var currentSongItemVisibility: Boolean
        get() = binding.currentSong.root.visibility == View.VISIBLE
        set(value) {
            binding.currentSong.root.visibility == if (value) View.VISIBLE else View.GONE
        }

    fun updateCurrentSong(song: Song?) {
        with(binding.currentSong) {
            title.text = song?.title ?: "-"
            text.text = song?.infoString() ?: "-"
        }
    }

    fun updateQueueTime(position: Int) {
        with(binding) {
            playerQueueSubHeader.text = buildUpNextAndQueueTimeText(position)
        }
    }

    private fun buildUpNextAndQueueTimeText(position: Int): String {
        val duration = MusicPlayerRemote.getQueueDurationMillis(position)
        return buildInfoString(
            resources.getString(R.string.up_next),
            readableDuration(duration)
        )
    }

    val scrollableArea: View? get() = binding.playerRecyclerView

    var shadowItemVisibility: Boolean
        get() = binding.queueTopShadow.visibility == View.VISIBLE
        set(value) {
            binding.queueTopShadow.visibility == if (value) View.VISIBLE else View.GONE
        }

    //region Color
    private var currentAnimator: Animator? = null
    private fun changeHighlightColor(oldColor: Int, newColor: Int, animated: Boolean = true) {
        if (animated) {
            currentAnimator?.end()
            currentAnimator?.cancel()
            currentAnimator =
                binding.playerQueueSubHeader.textColorTransitionAnimator(textColor(oldColor), textColor(newColor))
                    .apply {
                        duration = PHONOGRAPH_ANIM_TIME / 2
                        start()
                    }
        } else {
            binding.playerQueueSubHeader.setTextColor(darkenColor(newColor))
        }
    }

    private fun textColor(@ColorInt color: Int): Int {
        val defaultFooterColor = themeFooterColor(requireContext())
        val nightMode = requireContext().nightMode
        return if (color == defaultFooterColor) requireContext().secondaryTextColor(nightMode)
        else if (nightMode) lightenColor(color) else darkenColor(color)
    }
    //endregion

    private fun observeState() {
        observe(queueViewModel.queue) { queue ->
            playingQueueAdapter.dataset = queue
            playingQueueAdapter.current = MusicPlayerRemote.position
        }
        observe(queueViewModel.position) { position ->
            playingQueueAdapter.current = position
            withStarted {
                updateQueueTime(position)
                resetToCurrentPosition(false)
            }
        }
        observe(queueViewModel.currentSong) { song ->
            if (song != null) {
                withStarted { updateCurrentSong(song) }
            }
        }
        observe(queueViewModel.shuffleMode) {
            lifecycle.withCreated {
                playingQueueAdapter.dataset = MusicPlayerRemote.playingQueue
                playingQueueAdapter.current = MusicPlayerRemote.position
                updateQueueTime(MusicPlayerRemote.position)
                resetToCurrentPosition(false)
            }
        }
        observe(panelViewModel.colorChange) { (oldColor, newColor) ->
            withStarted {
                changeHighlightColor(oldColor, newColor, lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            }
        }
    }

    private inline fun <reified T> observe(
        flow: StateFlow<T>,
        state: Lifecycle.State = Lifecycle.State.CREATED,
        lifecycle: Lifecycle = this.lifecycle,
        scope: CoroutineScope = lifecycle.coroutineScope,
        flowCollector: FlowCollector<T>,
    ) {
        scope.launch {
            lifecycle.repeatOnLifecycle(state) {
                flow.collect(flowCollector)
            }
        }
    }

}