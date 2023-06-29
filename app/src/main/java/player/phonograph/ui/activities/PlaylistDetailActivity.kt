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
import mt.tint.viewtint.setBackgroundTint
import mt.util.color.primaryTextColor
import mt.util.color.secondaryDisabledTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.actions.menu.playlistToolbar
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.display.PlaylistSongDisplayAdapter
import player.phonograph.databinding.ActivityPlaylistDetailBinding
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Song
import player.phonograph.model.UIMode
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.model.totalDuration
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import util.phonograph.playlist.mediastore.moveItemViaMediastore
import util.phonograph.playlist.mediastore.removeFromPlaylistViaMediastore
import androidx.activity.viewModels
import androidx.core.graphics.BlendModeCompat
import androidx.core.widget.addTextChangedListener
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

        prepareRecyclerView()
        updateRecyclerView(editMode = false)
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
            model.currentMode.collect { mode ->
                switchMode(model.previousMode, mode)
                supportActionBar!!.title =
                    if (mode == UIMode.Editor)
                        "${model.playlist.value.name} [${getString(R.string.edit)}]"
                    else
                        model.playlist.value.name
            }
        }
        lifecycleScope.launch {
            model.playlist.collect { playlist ->
                model.fetchAllSongs(this@PlaylistDetailActivity)
                supportActionBar!!.title = playlist.name
                if (playlist !is SmartPlaylist &&
                    !PlaylistLoader.checkExistence(this@PlaylistDetailActivity, playlist.id)
                ) {
                    // File Playlist was deleted
                    finish()
                }
                updateDashboard(playlist, model.songs.value)
            }
        }
        lifecycleScope.launch {
            model.keyword.collect { word ->
                if (model.currentMode.value == UIMode.Search) {
                    model.searchSongs(this@PlaylistDetailActivity, word)
                }
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

    private fun prepareRecyclerView() {
        // FastScrollRecyclerView
        binding.recyclerView.setUpFastScrollRecyclerViewColor(this, accentColor)
        binding.recyclerView.setOnFastScrollStateChangeListener(
            object : OnFastScrollStateChangeListener {
                override fun onFastScrollStart() {
                    binding.dashBroad.setExpanded(false, false)
                    // hide dashboard instantly
                }

                override fun onFastScrollStop() {}
            }
        )
        // adapter
        adapter = PlaylistSongDisplayAdapter(this, cabController, ArrayList(), null)
    }

    private fun updateRecyclerView(editMode: Boolean) {

        if (!editMode) {
            adapter.editMode = false
            binding.recyclerView.also { rv ->
                rv.layoutManager = LinearLayoutManager(this)
                rv.adapter = adapter
            }
            adapter.onMove = { _, _ -> true }
            adapter.onDelete = {}
        } else {
            val playlist = model.playlist.value
            adapter.editMode = true
            binding.recyclerView.also { rv ->
                recyclerViewDragDropManager = RecyclerViewDragDropManager()
                recyclerViewDragDropManager!!.attachRecyclerView(rv)
                wrappedAdapter = recyclerViewDragDropManager!!.createWrappedAdapter(adapter)

                rv.adapter = wrappedAdapter
                rv.layoutManager = LinearLayoutManager(this)
                rv.itemAnimator = RefactoredDefaultItemAnimator()
            }
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
        }
    }

    private fun setUpDashBroad() {
        with(binding) {
            dashBroad.setBackgroundColor(primaryColor)
            dashBroad.addOnOffsetChangedListener { _, verticalOffset ->
                updateRecyclerviewPadding(verticalOffset)
            }
            updateRecyclerviewPadding(0)
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
                        model.updateCurrentMode(UIMode.Common)
                    } else {
                        editable.clear()
                    }
                }
                editQuery.setTextColor(textColor)
                editQuery.setHintTextColor(iconColor)
                editQuery.setBackgroundTint(textColor)
            }
            searchBox.editQuery.addTextChangedListener { editable ->
                if (editable != null) {
                    model.updateKeyword(editable.toString())
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

    private fun showSearchBar() {
        with(binding) {
            searchBar.visibility = VISIBLE
            searchBox.editQuery.setText(model.keyword.value)
            updateRecyclerviewPadding(0)
        }
    }

    private fun hideSearchBar() {
        with(binding) {
            searchBar.visibility = GONE
            searchBox.editQuery.setText("")
            updateRecyclerviewPadding(searchBar.height)
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
        playlistToolbar(menu, this, model, iconColor = primaryTextColor(primaryColor))
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


    @Synchronized
    fun switchMode(oldMode: UIMode, newMode: UIMode) {

        when (oldMode) {
            UIMode.Common -> when (newMode) {
                UIMode.Common -> {}
                UIMode.Editor -> {
                    updateRecyclerView(editMode = true)
                }

                UIMode.Search -> {
                    model.searchSongs(this, model.keyword.value)
                    showSearchBar()
                }
            }

            UIMode.Editor -> when (newMode) {
                UIMode.Common -> {
                    updateRecyclerView(editMode = false)
                }

                UIMode.Editor -> {}
                UIMode.Search -> {
                    updateRecyclerView(editMode = false)
                    model.searchSongs(this, model.keyword.value)
                    showSearchBar()
                }
            }

            UIMode.Search -> when (newMode) {
                UIMode.Common -> {
                    model.fetchAllSongs(this)
                    hideSearchBar()
                }

                UIMode.Editor -> {
                    model.fetchAllSongs(this)
                    updateRecyclerView(editMode = true)
                    hideSearchBar()
                }

                UIMode.Search -> {}
            }
        }
    }

    override fun onBackPressed() {
        when {
            model.currentMode.value == UIMode.Common -> super.onBackPressed()
            else                                     -> model.updateCurrentMode(UIMode.Common)
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
            adapter.dataset = emptyList()
            model.refreshPlaylist(this@PlaylistDetailActivity)
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
