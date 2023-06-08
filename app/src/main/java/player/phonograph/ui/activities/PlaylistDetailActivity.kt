/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener
import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.createToolbarCab
import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenDirStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenDirStorageAccessTool
import lib.phonograph.misc.OpenFileStorageAccessTool
import lib.phonograph.misc.menuProvider
import mt.tint.setActivityToolbarColorAuto
import mt.util.color.primaryTextColor
import mt.util.color.secondaryDisabledTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.actions.menu.playlistToolbar
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.display.PlaylistSongDisplayAdapter
import player.phonograph.databinding.ActivityPlaylistDetailBinding
import player.phonograph.mechanism.PlaylistsManagement
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.PlaylistDetailMode
import player.phonograph.model.Song
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.GeneratedPlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.model.totalDuration
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import util.phonograph.playlist.mediastore.moveItemViaMediastore
import util.phonograph.playlist.mediastore.removeFromPlaylistViaMediastore
import androidx.activity.viewModels
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PlaylistDetailActivity :
        AbsSlidingMusicPanelActivity(),
        IOpenFileStorageAccess,
        ICreateFileStorageAccess,
        IOpenDirStorageAccess {

    private lateinit var binding: ActivityPlaylistDetailBinding

    private val model: PlaylistModel by viewModels()

    private lateinit var adapter: PlaylistSongDisplayAdapter // init in OnCreate() -> setUpRecyclerView()

    // drag & edit
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null
    private var wrappedAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

    // for saf callback
    override val openFileStorageAccessTool: OpenFileStorageAccessTool =
        OpenFileStorageAccessTool()
    override val openDirStorageAccessTool: OpenDirStorageAccessTool =
        OpenDirStorageAccessTool()
    override val createFileStorageAccessTool: CreateFileStorageAccessTool =
        CreateFileStorageAccessTool()

    /* ********************
     *
     *  First Initialization
     *
     * ********************/

    override fun onCreate(savedInstanceState: Bundle?) {

        val playlist = intent.extras?.getParcelable<Playlist>(EXTRA_PLAYLIST)
        if (playlist == null) {
            finish()
        } else {
            model.initPlaylist(playlist)
        }

        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)

        openFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        openDirStorageAccessTool.register(lifecycle, activityResultRegistry)
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        lifecycle.addObserver(MediaStoreListener())

        super.onCreate(savedInstanceState)
        setUpToolbar()

        setUpRecyclerView()
        setUpDashBroad()

        observeData()
    }

    private fun observeData() {
        lifecycleScope.launch {
            model.songs.collect { songs ->
                adapter.dataset = songs
                binding.empty.visibility = if (songs.isEmpty()) VISIBLE else GONE
                updateDashboard(model.playlist.value, songs)
            }
        }

        lifecycleScope.launch {
            model.playlist.collect { playlist ->
                model.fetchSongs(this@PlaylistDetailActivity)
                supportActionBar!!.title = playlist.name
                if (playlist !is SmartPlaylist &&
                    !PlaylistsManagement.doesPlaylistExist(this@PlaylistDetailActivity, playlist.id)
                ) {
                    // File Playlist was deleted
                    finish()
                }
                updateDashboard(playlist, model.songs.value)
            }
        }
    }

    private lateinit var cab: ToolbarCab
    private lateinit var cabController: MultiSelectionCabController

    override fun createContentView(): View = wrapSlidingMusicPanel(binding.root)

    private fun setUpToolbar() {

        // multiselect cab
        cab = createToolbarCab(this, R.id.cab_stub, R.id.multi_selection_cab)
        cabController = MultiSelectionCabController(cab)

        binding.toolbar.setBackgroundColor(primaryColor)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu, this::setupMenuCallback))

        setActivityToolbarColorAuto(binding.toolbar)
    }

    private fun setUpRecyclerView() {
        binding.recyclerView.setUpFastScrollRecyclerViewColor(this, accentColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.recyclerView.setOnFastScrollStateChangeListener(
            object : OnFastScrollStateChangeListener {
                override fun onFastScrollStart() {
                    binding.dashBroad.setExpanded(false, false)
                    // hide dashboard instantly
                }

                override fun onFastScrollStop() {}
            }
        )
        // Init adapter
        adapter = PlaylistSongDisplayAdapter(this, cabController, ArrayList(), null)
        binding.recyclerView.adapter = adapter
    }

    private fun setUpDashBroad() {
        with(binding) {
            dashBroad.setBackgroundColor(primaryColor)
            dashBroad.addOnOffsetChangedListener { _, verticalOffset ->
                recyclerView.setPadding(
                    recyclerView.paddingLeft,
                    dashBroad.totalScrollRange + verticalOffset,
                    recyclerView.paddingRight,
                    recyclerView.paddingBottom
                )
            }
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop + dashBroad.height,
                recyclerView.paddingRight,
                recyclerView.paddingBottom
            )
        }

        // colors
        val textColor = secondaryTextColor(primaryColor)
        val iconColor = secondaryDisabledTextColor(primaryColor)
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
        }
    }

    private fun updateDashboard(playlist: Playlist, songs: List<Song>) {
        // text
        with(binding) {
            nameText.text = playlist.name
            songCountText.text = songs.size.toString()
            durationText.text = getReadableDurationString(songs.totalDuration())
            if (playlist is FilePlaylist) {
                pathText.text = playlist.associatedFilePath
            } else {
                pathText.visibility = GONE
                pathIcon.visibility = GONE
            }
        }
    }

    private fun setupMenu(menu: Menu) {
        val playlist: Playlist = model.playlist.value
        val iconColor = primaryTextColor(primaryColor)
        playlistToolbar(menu, this, playlist, iconColor, ::enterEditMode) {
            refresh(playlist)
        }
    }

    private fun refresh(playlist: Playlist) {
        if (playlist is GeneratedPlaylist) {
            playlist.refresh(this)
        }
        adapter.dataset = emptyList()
        model.fetchSongs(this)
    }

    private fun setupMenuCallback(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else              -> false
        }
    }

    private fun enterEditMode() {

        model.mode = PlaylistDetailMode.Editor
        adapter.editMode = true

        val playlist = model.playlist.value

        adapter.onMove = { fromPosition: Int, toPosition: Int ->
            runBlocking {
                moveItemViaMediastore(this@PlaylistDetailActivity, playlist.id, fromPosition, toPosition)
            }
        }
        adapter.onDelete = {
            runBlocking {
                removeFromPlaylistViaMediastore(this@PlaylistDetailActivity, adapter.dataset[it], playlist.id)
            }
        }

        with(binding) {
            supportActionBar!!.title = "${playlist.name} [${getString(R.string.edit)}]"

            recyclerViewDragDropManager = RecyclerViewDragDropManager()
            wrappedAdapter = recyclerViewDragDropManager!!.createWrappedAdapter(adapter)
            recyclerView.itemAnimator = RefactoredDefaultItemAnimator()
            recyclerView.adapter = wrappedAdapter
            recyclerViewDragDropManager!!.attachRecyclerView(binding.recyclerView)
        }
    }

    private fun exitEditMode() {
        model.mode = PlaylistDetailMode.Common
        adapter.editMode = false

        setUpRecyclerView()
        refresh(model.playlist.value)
    }

    override fun onBackPressed() {
        when (model.mode) {
            PlaylistDetailMode.Editor -> exitEditMode()
            PlaylistDetailMode.Search -> {} //todo
            PlaylistDetailMode.Common -> {
                if (cabController.dismiss()) return else super.onBackPressed()
            }
        }
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
        binding.recyclerView.adapter = null
    }

    override fun onPause() {
        super.onPause()
        recyclerViewDragDropManager?.cancelDrag()
    }

    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() {
            refresh(model.playlist.value)
        }
    }

    /* *******************
     *   companion object
     * *******************/

    companion object {
        private const val TAG = "PlaylistDetail"
        const val EXTRA_PLAYLIST = "extra_playlist"
    }
}
