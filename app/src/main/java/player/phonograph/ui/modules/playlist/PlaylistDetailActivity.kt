/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.modules.playlist

import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener
import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.phonograph.misc.menuProvider
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
import player.phonograph.mechanism.broadcast.PlaylistsModifiedReceiver
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.ui.UIMode
import player.phonograph.repo.loader.Playlists
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.util.observe
import player.phonograph.util.parcelable
import player.phonograph.util.text.readableDuration
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.ui.hideKeyboard
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import player.phonograph.util.ui.showKeyboard
import util.theme.color.primaryTextColor
import util.theme.color.secondaryDisabledTextColor
import util.theme.color.secondaryTextColor
import util.theme.view.menu.tintOverflowButtonColor
import util.theme.view.menu.tintToolbarMenuActionIcons
import util.theme.view.setBackgroundTint
import util.theme.view.toolbar.setToolbarColor
import androidx.activity.addCallback
import androidx.core.graphics.BlendModeCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlaylistDetailActivity :
        AbsSlidingMusicPanelActivity(),
        IOpenFileStorageAccessible,
        ICreateFileStorageAccessible,
        IOpenDirStorageAccessible {

    private lateinit var binding: ActivityPlaylistDetailBinding

    private val viewModel: PlaylistDetailViewModel by viewModel { parametersOf(parseIntent(intent), emptyList<Song>()) }

    private lateinit var adapter: PlaylistSongDisplayAdapter // init in OnCreate() -> setUpRecyclerView()

    // drag & edit
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null
    private var wrappedAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

    // for saf callback
    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()


    /* ********************
     *
     *  First Initialization
     *
     * ********************/

    override fun onCreate(savedInstanceState: Bundle?) {

        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)

        registerActivityResultLauncherDelegate(
            createFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
            openFileStorageAccessDelegate,
        )

        lifecycle.addObserver(MediaStoreListener())
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(playlistsModifiedReceiver, PlaylistsModifiedReceiver.filter)

        super.onCreate(savedInstanceState)
        setUpToolbar()

        prepareRecyclerView()
        setUpDashBroad()

        initialize()

        observeData()

        setupOnBackPressCallback()
    }


    private fun initialize() {
        val playlist = viewModel.playlist
        supportActionBar!!.title = playlist.name

        lifecycleScope.launch {
            if (!checkExistence(playlist)) finish()  // File Playlist was deleted
            execute(Fetch)
        }
    }

    private fun observeData() {
        observe(viewModel.items) { songs ->
            adapter.dataset = songs
            binding.empty.visibility = if (songs.isEmpty()) VISIBLE else GONE
        }
        observe(viewModel.currentMode) { mode ->
            supportActionBar!!.title =
                if (mode == UIMode.Editor)
                    "${viewModel.playlist.name} [${getString(R.string.edit)}]"
                else
                    viewModel.playlist.name
            updateBannerVisibility(mode)
            @SuppressLint("NotifyDataSetChanged")
            adapter.notifyDataSetChanged()
            if (mode == UIMode.Common) execute(Refresh(true))
        }
        observe(viewModel.totalCount) {
            with(binding) {
                @SuppressLint("SetTextI18n")
                songCountText.text = it.toString()
            }
        }
        observe(viewModel.totalDuration) { duration ->
            with(binding) {
                durationText.text = readableDuration(duration)
            }
        }
    }

    private fun setupOnBackPressCallback() {
        onBackPressedDispatcher.addCallback {
            if (viewModel.currentMode.value != UIMode.Common) {
                execute(UpdateMode(UIMode.Common))
            } else {
                remove()
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun createContentView(): View = wrapSlidingMusicPanel(binding.root)

    private fun setUpToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setToolbarColor(binding.toolbar, primaryColor())
    }

    private fun prepareRecyclerView() {
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
        adapter = PlaylistSongDisplayAdapter(
            this,
            viewModel,
            PlaylistSongDisplayAdapter.PlaylistSongDisplayPresenter { adapter.menuProvider }
        )
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
            recyclerView.itemAnimator = RefactoredDefaultItemAnimator()
        }
    }

    private fun setUpDashBroad() {
        with(binding) {
            dashBroad.setBackgroundColor(primaryColor())
            dashBroad.addOnOffsetChangedListener { _, verticalOffset ->
                updateRecyclerviewPadding(verticalOffset)
            }
            updateRecyclerviewPadding(0)
        }

        // colors
        val textColor = secondaryTextColor(primaryColor())
        val iconColor = secondaryDisabledTextColor(primaryColor())
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


            val playlist = viewModel.playlist
            nameText.text = playlist.name
            pathText.text = playlist.location.text(this@PlaylistDetailActivity)


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
                        execute(UpdateMode(UIMode.Common))
                    } else {
                        editQuery.editableText.clear()
                    }
                }
                editQuery.setTextColor(textColor)
                editQuery.setHintTextColor(iconColor)
                editQuery.setBackgroundTint(textColor)
                editQuery.addTextChangedListener { editable ->
                    if (editable != null) {
                        execute(Search(editable.toString()))
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
        val iconColor = primaryTextColor(panelViewModel.activityColor.value)
        PlaylistToolbarMenuProvider(::execute).inflateMenu(menu, this, viewModel.playlist, iconColor)
        tintToolbarMenuActionIcons(menu, iconColor)
        tintOverflowButtonColor(this, iconColor)
    }

    private fun execute(action: PlaylistAction): Boolean {
        lifecycleScope.launch {
            viewModel.execute(this@PlaylistDetailActivity, action)
        }
        return true
    }

    /* *******************
     *
     *    States Changed
     *
     * *******************/

    override fun onDestroy() {
        super.onDestroy()
        wrappedAdapter?.let {
            WrapperAdapterUtils.releaseAll(it)
            wrappedAdapter = null
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playlistsModifiedReceiver)
        binding.recyclerView.adapter = null
    }

    override fun onPause() {
        super.onPause()
        recyclerViewDragDropManager?.cancelDrag()
    }

    private fun refreshIfInNeed() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (viewModel.currentMode.value != UIMode.Editor) {
                lifecycle.withCreated {
                    // adapter.dataset = emptyList()
                    execute(Refresh(fetch = true))
                }
            }
        }
    }


    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() = refreshIfInNeed()
    }

    private val playlistsModifiedReceiver = object : PlaylistsModifiedReceiver() {
        override fun onPlaylistChanged(context: Context, intent: Intent) = refreshIfInNeed()
    }

    private suspend fun checkExistence(playlist: Playlist): Boolean =
        !(playlist.location is FilePlaylistLocation && !Playlists.exists(this, playlist.location))

    /* *******************
     *   companion object
     * *******************/

    companion object {
        private const val TAG = "PlaylistDetail"
        private const val EXTRA_PLAYLIST = "extra_playlist"
        fun launchIntent(from: Context, playlist: Playlist): Intent =
            Intent(from, PlaylistDetailActivity::class.java).apply {
                putExtra(EXTRA_PLAYLIST, playlist)
            }

        private fun parseIntent(intent: Intent) = intent.extras?.parcelable<Playlist>(EXTRA_PLAYLIST)
    }
}
