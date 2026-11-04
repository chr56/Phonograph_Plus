/*
 *  Copyright (c) 2021~2026 chr_56
 */

package player.phonograph.ui.modules.playlist

import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener
import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.launcher.CreateFileStorageAccessDelegate
import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenDirStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDirStorageAccessDelegate
import lib.storage.launcher.OpenFileStorageAccessDelegate
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import player.phonograph.R
import player.phonograph.databinding.ActivityPlaylistDetailBinding
import player.phonograph.foundation.compat.parcelable
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Song
import player.phonograph.model.playlist.PLAYLIST_TYPE_FAVORITE
import player.phonograph.model.playlist.PLAYLIST_TYPE_LAST_ADDED
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.model.ui.UIMode
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.ui.resource.Durations
import player.phonograph.ui.resource.Texts
import player.phonograph.ui.theme.ThemeSettingsDelegate.accentColor
import player.phonograph.ui.theme.ThemeSettingsDelegate.primaryColor
import player.phonograph.ui.theme.getTintedDrawable
import player.phonograph.ui.theme.secondaryTextColorOn
import player.phonograph.ui.theme.setUpFastScrollRecyclerViewColor
import player.phonograph.ui.theme.textColorOn
import player.phonograph.ui.util.BottomViewWindowInsetsController
import player.phonograph.ui.util.applyControllableWindowInsetsAsBottomView
import player.phonograph.ui.util.hideKeyboard
import player.phonograph.ui.util.inflateToolbarEntries
import player.phonograph.ui.util.menuProvider
import player.phonograph.ui.util.observe
import player.phonograph.ui.util.showKeyboard
import util.theme.view.menu.tintOverflowButtonColor
import util.theme.view.menu.tintOverflowMenuItems
import util.theme.view.menu.tintToolbarMenuActionIcons
import util.theme.view.setBackgroundTint
import util.theme.view.toolbar.setToolbarColor
import androidx.activity.addCallback
import androidx.core.graphics.BlendModeCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PlaylistDetailActivity :
        AbsSlidingMusicPanelActivity(),
        IOpenFileStorageAccessible, ICreateFileStorageAccessible, IOpenDirStorageAccessible {

    private lateinit var binding: ActivityPlaylistDetailBinding
    private val viewModel: PlaylistDetailViewModel by viewModel { parametersOf(parseIntent(intent), emptyList<Song>()) }

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()

    private lateinit var adapter: PlaylistSongDisplayAdapter // init in OnCreate() -> setUpRecyclerView()
    private var wrappedAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null
    private var closingUnavailablePlaylist = false

    override fun createContentView(): View = wrapSlidingMusicPanel(binding.root)

    override fun onCreate(savedInstanceState: Bundle?) {
        registerActivityResultLauncherDelegate(
            createFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
            openFileStorageAccessDelegate,
        )

        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater) // must call before super due to `createContentView()`

        super.onCreate(savedInstanceState)

        setUpToolbar()
        setUpMainContent()

        observeData()

        lifecycle.addObserver(MediaStoreListener())
        playlistsEventReceiver.registerWithLifecycle(lifecycle)
        favoritesEventReceiver.registerWithLifecycle(lifecycle)

        setupOnBackPressCallback()
        execute(PlaylistAction.Load(scan = true))
    }

    private fun observeData() {
        observe(viewModel.items) { songs ->
            if (songs != adapter.dataset) adapter.clearSelection()
            adapter.dataset = songs
            binding.empty.visibility = if (songs.isEmpty()) VISIBLE else GONE
        }
        observe(viewModel.currentMode) { mode ->
            updateBannerVisibility(mode)
            @SuppressLint("NotifyDataSetChanged")
            adapter.notifyDataSetChanged()
        }
        observe(
            combine(viewModel.playlist, viewModel.currentMode) { playlist, mode -> playlist to mode }
        ) { (playlist, mode) ->
            binding.toolbar.title =
                if (mode == UIMode.Editor && playlist != null)
                    "${playlist.name} [${getString(R.string.action_edit)}]"
                else
                    playlist?.name.orEmpty()
        }
        observe(viewModel.playlist) { playlist ->
            if (playlist == null) {
                if (!closingUnavailablePlaylist) {
                    closingUnavailablePlaylist = true
                    finish()
                }
            } else {
                binding.nameText.text = playlist.name
                binding.pathText.text = Texts.playlist(resources, playlist.location)
            }
            invalidateMenu()
        }
        observe(viewModel.loadErrors) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        observe(viewModel.totalCount) { binding.songCountText.text = it.toString() }
        observe(viewModel.totalDuration) { duration -> binding.durationText.text = Durations.short(duration) }
    }

    private fun setupOnBackPressCallback() {
        onBackPressedDispatcher.addCallback {
            if (viewModel.currentMode.value != UIMode.Common) {
                execute(PlaylistAction.UpdateMode(UIMode.Common))
            } else {
                remove()
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setUpToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            if (!isTaskRoot) onBackPressedDispatcher.onBackPressed()
        }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setToolbarColor(binding.toolbar, primaryColor())
    }

    private lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController
    private fun setUpMainContent() {
        binding.toolbar.title = viewModel.playlist.value?.name
        // FastScrollRecyclerView
        binding.recyclerView.setUpFastScrollRecyclerViewColor(this, accentColor())
        binding.recyclerView.setOnFastScrollStateChangeListener(
            object : OnFastScrollStateChangeListener {
                override fun onFastScrollStart() {
                    binding.dashBroad.setExpanded(false, false)
                    // hide dashboard instantly
                }

                override fun onFastScrollStop() {}
            }
        )
        // Adapter
        adapter = PlaylistSongDisplayAdapter(this, viewModel)
        // DragDropAdapter
        binding.recyclerView.also { recyclerView ->
            recyclerViewDragDropManager = RecyclerViewDragDropManager().apply {
                attachRecyclerView(recyclerView)
                setInitiateOnTouch(true)
                setInitiateOnLongPress(false)
                wrappedAdapter = createWrappedAdapter(adapter)
            }
            recyclerView.adapter = wrappedAdapter
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.itemAnimator = null
        }

        // WindowInsets
        bottomViewWindowInsetsController = binding.recyclerView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isMiniPlayerHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }

        //
        // DashBroad
        //
        with(binding) {
            dashBroad.setBackgroundColor(primaryColor())
            dashBroad.addOnOffsetChangedListener { _, verticalOffset ->
                updateRecyclerviewPadding(verticalOffset)
            }
            updateRecyclerviewPadding(0)
        }
        // colors
        val textColor = secondaryTextColorOn(this, primaryColor())
        val iconColor = secondaryTextColorOn(this, primaryColor())
        with(binding) {
            nameIcon.setImageDrawable(
                getTintedDrawable(
                    R.drawable.ic_description_white_24dp,
                    iconColor,
                    BlendModeCompat.SRC_ATOP
                )
            )
            songCountIcon.setImageDrawable(
                getTintedDrawable(
                    R.drawable.ic_music_note_white_24dp,
                    iconColor,
                    BlendModeCompat.SRC_ATOP
                )
            )
            durationIcon.setImageDrawable(
                getTintedDrawable(
                    R.drawable.ic_timer_white_24dp,
                    iconColor,
                    BlendModeCompat.SRC_ATOP
                )
            )
            pathIcon.setImageDrawable(
                getTintedDrawable(
                    R.drawable.ic_file_music_white_24dp,
                    iconColor,
                    BlendModeCompat.SRC_ATOP
                )
            )

            icon.setImageDrawable(
                getTintedDrawable(
                    R.drawable.ic_queue_music_white_24dp,
                    textColor
                )
            )

            nameText.setTextColor(textColor)
            songCountText.setTextColor(textColor)
            durationText.setTextColor(textColor)
            pathText.setTextColor(textColor)

            with(searchBox) {
                searchBadge.setImageDrawable(
                    getTintedDrawable(R.drawable.ic_search_white_24dp, textColor)
                )
                close.setImageDrawable(
                    getTintedDrawable(R.drawable.ic_close_white_24dp, textColor)
                )
                close.setOnClickListener {
                    val editable = editQuery.editableText
                    if (editable.isEmpty()) {
                        execute(PlaylistAction.UpdateMode(UIMode.Common))
                    } else {
                        editQuery.editableText.clear()
                    }
                }
                editQuery.setTextColor(textColor)
                editQuery.setHintTextColor(iconColor)
                editQuery.setBackgroundTint(textColor)
                editQuery.setText(viewModel.query.value)
                editQuery.addTextChangedListener { editable ->
                    if (editable != null) {
                        execute(PlaylistAction.Search(editable.toString()))
                    }
                }
            }
        }

    }


    private fun updateRecyclerviewPadding(verticalOffset: Int) {
        with(binding) {
            val paddingTop = dashBroad.totalScrollRange + verticalOffset
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                paddingTop,
                recyclerView.paddingRight,
                recyclerView.paddingBottom
            )
        }
    }

    private fun updateBannerVisibility(mode: UIMode) {
        with(binding) {
            // Search Bar
            val searchBarVisibility = mode == UIMode.Search
            searchBar.visibility = if (searchBarVisibility) VISIBLE else GONE
            // Dashboard
            val statsBarVisibility = mode != UIMode.Search
            statsBar.visibility = if (statsBarVisibility) VISIBLE else GONE
            // IME
            if (searchBarVisibility) {
                showKeyboard(this@PlaylistDetailActivity, searchBox.editQuery)
            } else {
                hideKeyboard(this@PlaylistDetailActivity, searchBox.editQuery)
            }
        }
    }

    private fun setupMenu(menu: Menu) {
        val iconColor = textColorOn(this, panelViewModel.activityColor.value)
        val item = viewModel.playlist.value ?: return
        val entries = buildList {
            add(PlaylistDetailActionEntries.Play(this@PlaylistDetailActivity, item))
            add(PlaylistDetailActionEntries.ShufflePlaylist(this@PlaylistDetailActivity, item))
            add(PlaylistDetailActionEntries.PlayNext(this@PlaylistDetailActivity, item))
            add(PlaylistDetailActionEntries.AddToPlayingQueue(this@PlaylistDetailActivity, item))
            add(PlaylistDetailActionEntries.AddToPlaylist(this@PlaylistDetailActivity, item))
            if (!item.isVirtual()) {
                add(PlaylistDetailActionEntries.Rename(this@PlaylistDetailActivity, item))
            }
            add(PlaylistDetailActionEntries.Delete(this@PlaylistDetailActivity, item))
            add(PlaylistDetailActionEntries.TagEditor(this@PlaylistDetailActivity, item))
            add(PlaylistDetailActionEntries.SavePlaylist(this@PlaylistDetailActivity, item))
            add(PlaylistDetailActionEntries.Search(this@PlaylistDetailActivity, ::execute))
            add(PlaylistDetailActionEntries.Refresh(this@PlaylistDetailActivity, ::execute))
            if (!item.isVirtual()) {
                add(PlaylistDetailActionEntries.Edit(this@PlaylistDetailActivity, ::execute))
            } else {
                val location = item.location
                if (location is VirtualPlaylistLocation) {
                    when (location.type) {
                        PLAYLIST_TYPE_LAST_ADDED ->
                            add(PlaylistDetailActionEntries.LastAddedInterval(this@PlaylistDetailActivity, ::execute))

                        PLAYLIST_TYPE_FAVORITE   ->
                            add(PlaylistDetailActionEntries.Clean(this@PlaylistDetailActivity, item))

                        else                     -> {}
                    }
                }
            }
        }
        inflateToolbarEntries(this, menu, iconColor, entries)
        tintToolbarMenuActionIcons(menu, iconColor)
        tintOverflowButtonColor(this, iconColor)
        tintOverflowMenuItems(binding.toolbar, accentColor())
    }

    private fun execute(action: PlaylistAction): Boolean {
        lifecycleScope.launch {
            viewModel.execute(this@PlaylistDetailActivity, action)
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        wrappedAdapter?.let {
            WrapperAdapterUtils.releaseAll(it)
            wrappedAdapter = null
        }
        binding.recyclerView.adapter = null
    }

    override fun onPause() {
        super.onPause()
        recyclerViewDragDropManager?.cancelDrag()
    }

    private inner class MediaStoreListener : EventHub.LifeCycleEventReceiver(this, EventHub.EVENT_MUSIC_LIBRARY_CHANGED) {
        override fun onEventReceived(context: Context, intent: Intent) {
            execute(PlaylistAction.Load())
        }
    }

    private val playlistsEventReceiver =
        EventHub.LifeCycleEventReceiver(this, EventHub.EVENT_PLAYLISTS_CHANGED) { _, _ ->
            execute(PlaylistAction.Load())
        }

    private val favoritesEventReceiver =
        EventHub.LifeCycleEventReceiver(this, EventHub.EVENT_FAVORITES_CHANGED) { _, _ ->
            if (viewModel.playlist.value?.location == VirtualPlaylistLocation.Favorite) {
                execute(PlaylistAction.Load())
            }
        }

    companion object {
        private const val EXTRA_PLAYLIST = "extra_playlist"
        fun launchIntent(from: Context, playlist: Playlist): Intent =
            Intent(from, PlaylistDetailActivity::class.java).apply {
                putExtra(EXTRA_PLAYLIST, playlist)
            }

        private fun parseIntent(intent: Intent) = intent.extras?.parcelable<Playlist>(EXTRA_PLAYLIST)
    }
}
