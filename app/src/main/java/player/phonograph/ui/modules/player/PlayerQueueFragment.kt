/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.player

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.github.chr56.android.menu_model.MenuContext
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.databinding.FragmentQueueBinding
import player.phonograph.mechanism.actions.ActionMenuProviders
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Song
import player.phonograph.model.service.RepeatMode
import player.phonograph.model.service.ShuffleMode
import player.phonograph.model.ui.UnarySlidingUpPanelProvider
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.QueueManager
import player.phonograph.ui.dialogs.QueueSnapshotsDialog
import player.phonograph.ui.modules.panel.AbsMusicServiceFragment
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.ui.modules.playlist.dialogs.CreatePlaylistDialogActivity
import player.phonograph.util.text.buildInfoString
import player.phonograph.util.text.infoString
import player.phonograph.util.text.readableDuration
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.theme.themeIconColor
import player.phonograph.util.theme.tintButtons
import player.phonograph.util.ui.PHONOGRAPH_ANIM_TIME
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import player.phonograph.util.ui.textColorTransitionAnimator
import util.theme.color.darkenColor
import util.theme.color.lightenColor
import util.theme.color.primaryTextColor
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
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
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

    companion object {
        private const val ARGUMENT_WITH_SHADOW = "arg_with_shadow"
        private const val ARGUMENT_WITH_ACTION_BUTTONS = "arg_with_action_buttons"
        private const val ARGUMENT_DISPLAY_CURRENT_SONG = "arg_display_current_song"

        fun newInstance(
            withShadow: Boolean = false,
            withActionButtons: Boolean = true,
            displayCurrentSong: Boolean = true,
        ) = PlayerQueueFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARGUMENT_WITH_SHADOW, withShadow)
                putBoolean(ARGUMENT_WITH_ACTION_BUTTONS, withActionButtons)
                putBoolean(ARGUMENT_DISPLAY_CURRENT_SONG, displayCurrentSong)
            }
        }
    }

    private var _viewBinding: FragmentQueueBinding? = null
    private val binding: FragmentQueueBinding get() = _viewBinding!!

    private val panelViewModel: PanelViewModel by viewModel(ownerProducer = { requireActivity() })

    private var argumentWithShadow: Boolean = false
    private var argumentWithActionButtons: Boolean = true
    private var argumentDisplayCurrentSong: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(MediaStoreListener())
        arguments?.let { args ->
            argumentWithShadow = args.getBoolean(ARGUMENT_WITH_SHADOW, false)
            argumentWithActionButtons = args.getBoolean(ARGUMENT_WITH_ACTION_BUTTONS, true)
            argumentDisplayCurrentSong = args.getBoolean(ARGUMENT_DISPLAY_CURRENT_SONG, true)
        }
    }

    override fun onInflate(
        context: Context,
        attrs: AttributeSet,
        savedInstanceState: Bundle?,
    ) {
        super.onInflate(context, attrs, savedInstanceState)
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.player_queue)
        try {
            argumentWithShadow = typedArray.getBoolean(R.styleable.player_queue_withShadow, false)
            argumentWithActionButtons = typedArray.getBoolean(R.styleable.player_queue_withActionButtons, true)
            argumentDisplayCurrentSong = typedArray.getBoolean(R.styleable.player_queue_displayCurrentSong, true)
        } finally {
            typedArray.recycle()
        }
    }

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
        initHeaderToolbar()

        binding.currentSong.root.visibility = if (!argumentDisplayCurrentSong) View.GONE else View.VISIBLE
        binding.queueTopShadow.visibility = if (!argumentWithShadow) View.GONE else View.VISIBLE

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


    private fun initHeaderToolbar() {
        val context = requireContext()

        val color = themeIconColor(context)
        val repeatMode = queueViewModel.repeatMode.value
        val shuffleMode = queueViewModel.shuffleMode.value

        binding.playerQueueSubHeader.setTextColor(color)
        context.attach(binding.playerQueueToolbar.menu) {
            rootMenu.clear()
            if (argumentWithActionButtons) setupToolbarModeActions(
                repeatMode,
                { MusicPlayerRemote.cycleRepeatMode() },
                shuffleMode,
                { MusicPlayerRemote.toggleShuffleMode() },
                color
            )
            setupToolbarOverflowMenu()
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
            binding.playerQueueSubHeader.setTextColor(textColor(newColor))
        }
    }

    private fun textColor(@ColorInt color: Int): Int {
        val defaultFooterColor = themeFooterColor(requireContext())
        val nightMode = requireContext().nightMode
        return if (color == defaultFooterColor) requireContext().primaryTextColor(nightMode)
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
                val context = requireContext()
                playingQueueAdapter.dataset = MusicPlayerRemote.playingQueue
                playingQueueAdapter.current = MusicPlayerRemote.position
                updateQueueTime(MusicPlayerRemote.position)
                resetToCurrentPosition(false)
                updateShuffleModeIcon(context, it, context.primaryTextColor(context.nightMode))
            }
        }
        observe(queueViewModel.repeatMode) {
            lifecycle.withCreated {
                val context = requireContext()
                updateRepeatModeIcon(context, it, context.primaryTextColor(context.nightMode))
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

    //region Toolbar Actions
    private fun lookupRepeatModeIcon(repeatMode: RepeatMode, color: Int): Drawable? =
        getTintedDrawable(
            when (repeatMode) {
                RepeatMode.NONE               -> R.drawable.ic_repeat_off_white_24dp
                RepeatMode.REPEAT_QUEUE       -> R.drawable.ic_repeat_white_24dp
                RepeatMode.REPEAT_SINGLE_SONG -> R.drawable.ic_repeat_one_white_24dp
            }, color
        )

    private fun lookupShuffleModeIcon(shuffleMode: ShuffleMode, color: Int): Drawable? =
        getTintedDrawable(
            when (shuffleMode) {
                ShuffleMode.NONE    -> R.drawable.ic_shuffle_disabled_white_24dp
                ShuffleMode.SHUFFLE -> R.drawable.ic_shuffle_white_24dp
            }, color
        )

    private var repeatModeItem: MenuItem? = null
    private var shuffleModeItem: MenuItem? = null

    private fun MenuContext.setupToolbarModeActions(
        repeatMode: RepeatMode,
        toggleRepeatMode: () -> Boolean,
        shuffleMode: ShuffleMode,
        toggleShuffleMode: () -> Boolean,
        color: Int,
    ) {
        repeatModeItem = menuItem(getString(R.string.action_repeat_mode)) {
            icon = lookupRepeatModeIcon(repeatMode, color)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick { toggleRepeatMode() }
        }
        shuffleModeItem = menuItem(getString(R.string.action_shuffle_mode)) {
            icon = lookupShuffleModeIcon(shuffleMode, color)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick { toggleShuffleMode() }
        }
    }

    private fun updateRepeatModeIcon(context: Context, repeatMode: RepeatMode, color: Int) {
        repeatModeItem?.icon = lookupRepeatModeIcon(repeatMode, color)
    }

    private fun updateShuffleModeIcon(context: Context, shuffleMode: ShuffleMode, color: Int) {
        shuffleModeItem?.icon = lookupShuffleModeIcon(shuffleMode, color)
    }

    private fun MenuContext.setupToolbarOverflowMenu() {
        menuItem {
            title = context.getString(R.string.action_clear_playing_queue)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                MusicPlayerRemote.pauseSong()
                MusicPlayerRemote.queueManager.clearQueue()
                true
            }
        }
        menuItem {
            title = context.getString(R.string.action_save_playing_queue)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                context.startActivity(
                    CreatePlaylistDialogActivity.Parameter.buildLaunchingIntentForCreating(
                        context, MusicPlayerRemote.playingQueue
                    )
                )
                true
            }
        }
        menuItem {
            title = context.getString(R.string.playing_queue_history)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                QueueSnapshotsDialog().show(childFragmentManager, "QUEUE_SNAPSHOTS")
                true
            }
        }
        menuItem {
            title = context.getString(R.string.action_clean_missing_items)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.action_clean)
                    .setMessage(R.string.action_clean_missing_items)
                    .setPositiveButton(context.getString(android.R.string.ok)) { dialog, _ ->
                        val queueManager: QueueManager = GlobalContext.get().get()
                        queueManager.clean()
                        dialog.dismiss()
                    }
                    .setNegativeButton(context.getString(android.R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .tintButtons()
                    .show()
                true
            }
        }
    }
    //endregion

}