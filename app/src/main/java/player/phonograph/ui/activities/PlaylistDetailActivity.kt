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
import mt.tint.setActivityToolbarColorAuto
import mt.util.color.primaryTextColor
import mt.util.color.secondaryDisabledTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.actions.menu.playlistToolbar
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.display.PlaylistSongAdapter
import player.phonograph.databinding.ActivityPlaylistDetailBinding
import lib.phonograph.misc.menuProvider
import player.phonograph.model.Song
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.GeneratedPlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.model.totalDuration
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.mechanism.PlaylistsManagement
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import util.phonograph.playlist.mediastore.moveItemViaMediastore
import util.phonograph.playlist.mediastore.removeFromPlaylistViaMediastore
import androidx.activity.viewModels
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import kotlinx.coroutines.runBlocking

class PlaylistDetailActivity :
        AbsSlidingMusicPanelActivity(),
        IOpenFileStorageAccess,
        ICreateFileStorageAccess,
        IOpenDirStorageAccess {

    private lateinit var binding: ActivityPlaylistDetailBinding

    private val model: PlaylistModel by viewModels()

    private lateinit var adapter: PlaylistSongAdapter // init in OnCreate() -> setUpRecyclerView()

    // drag & edit
    private var editMode: Boolean = false
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
        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        setUpToolbar()
        super.onCreate(savedInstanceState)
        addMenuProvider(menuProvider(this::setupMenu, this::setupMenuCallback))

        setActivityToolbarColorAuto(binding.toolbar)

        model.playlist.observe(this) {
            model.fetchPlaylist(this, playlistCallBack)
        }
        model.playlist.value = intent.extras!!.getParcelable(EXTRA_PLAYLIST)!!

        // multiselect cab
        cab = createToolbarCab(this, R.id.cab_stub, R.id.multi_selection_cab)
        cabController = MultiSelectionCabController(cab)

        openFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        openDirStorageAccessTool.register(lifecycle, activityResultRegistry)
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)

        setUpRecyclerView()
        setUpDashBroad()
    }

    lateinit var cab: ToolbarCab
    lateinit var cabController: MultiSelectionCabController

    override fun createContentView(): View = wrapSlidingMusicPanel(binding.root)

    private fun setUpToolbar() {
        binding.toolbar.setBackgroundColor(primaryColor)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
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

        model.playlist.value ?: FilePlaylist()
        // Init adapter
        adapter = PlaylistSongAdapter(
            this, cabController, ArrayList()
        ) {}
        binding.recyclerView.adapter = adapter

        model.isRecyclerViewReady = true
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
    }

    private val playlistCallBack: PlaylistCallback
        get() = { playlist: Playlist, songs: List<Song> ->
            adapter.dataset = songs
            binding.empty.visibility = if (songs.isEmpty()) VISIBLE else GONE
            supportActionBar!!.title = playlist.name
            if (playlist !is SmartPlaylist && !PlaylistsManagement.doesPlaylistExist(this, playlist.id)) {
                // File Playlist was deleted
                finish()
            }
            updateDashboard()
        }

    private fun updateDashboard() {

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
        // text

        val playlist = model.playlist.value
        with(binding) {
            nameText.text = playlist?.name ?: "-"
            songCountText.text = adapter.dataset.size.toString()
            durationText.text = getReadableDurationString(adapter.dataset.totalDuration())
            if (playlist is FilePlaylist) {
                pathText.text = playlist.associatedFilePath
            } else {
                pathText.visibility = GONE
                pathIcon.visibility = GONE
            }
        }
    }

    private fun setupMenu(menu: Menu) {
        val playlist: Playlist = model.playlist.value ?: FilePlaylist()
        val iconColor = primaryTextColor(primaryColor)
        playlistToolbar(menu, this, playlist, iconColor, ::enterEditMode) {
            refreshCallback(playlist)
        }
    }

    private fun refreshCallback(playlist: Playlist) {
        if (playlist is GeneratedPlaylist) {
            playlist.refresh(this)
        }
        adapter.dataset = emptyList()
        model.triggerUpdate()
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
        val playlist = model.playlist.value ?: return

        editMode = true
        adapter.editMode = true

        adapter.onMove = { fromPosition: Int, toPosition: Int ->
            runBlocking {
                moveItemViaMediastore(this@PlaylistDetailActivity, playlist.id, fromPosition, toPosition)
            }
        }
        adapter.onDelete = {
            runBlocking{
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
        editMode = false
        adapter.editMode = false

        setUpRecyclerView()
        model.triggerUpdate()
    }

    override fun onBackPressed() {
        if (editMode) {
            exitEditMode()
            return
        }
        if (cabController.dismiss()) return else super.onBackPressed()
    }

    /* *******************
     *
     *    States Changed
     *
     * *******************/

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        model.triggerUpdate()
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

    /* *******************
     *   companion object
     * *******************/

    companion object {
        private const val TAG = "PlaylistDetail"
        const val EXTRA_PLAYLIST = "extra_playlist"
    }
}
