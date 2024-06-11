/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.modules.playlist2

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
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Song
import player.phonograph.model.UIMode
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.playlist2.Playlist
import player.phonograph.model.totalDuration
import player.phonograph.repo.mediastore.loaders.PlaylistLoader2
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.parcelable
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import util.theme.color.primaryTextColor
import util.theme.color.secondaryDisabledTextColor
import util.theme.color.secondaryTextColor
import util.theme.view.setBackgroundTint
import util.theme.view.toolbar.setToolbarColor
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
import kotlinx.coroutines.launch

class PlaylistDetailActivity :
        AbsSlidingMusicPanelActivity(),
        IOpenFileStorageAccessible,
        ICreateFileStorageAccessible,
        IOpenDirStorageAccessible {

    private lateinit var binding: ActivityPlaylistDetailBinding

    private val model: PlaylistDetailViewModel by viewModel { parametersOf(parseIntent(intent)) }

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

        super.onCreate(savedInstanceState)
        setUpToolbar()

        prepareRecyclerView()
        setUpDashBroad()

        observeData()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeData() {
        lifecycleScope.launch {
            model.songs.collect { songs ->
                if (model.currentMode.value != UIMode.Search) {
                    adapter.dataset = songs
                    binding.empty.visibility = if (songs.isEmpty()) VISIBLE else GONE
                    updateDashboard(model.playlist.value, songs)
                }
            }
        }
        lifecycleScope.launch {
            model.currentMode.collect { mode ->
                supportActionBar!!.title =
                    if (mode == UIMode.Editor)
                        "${model.playlist.value.name} [${getString(R.string.edit)}]"
                    else
                        model.playlist.value.name
                updateSearchBarVisibility(mode == UIMode.Search)
                adapter.notifyDataSetChanged()
                if (mode == UIMode.Search) {
                    model.searchSongs(model.keyword.value)
                }
            }
        }
        lifecycleScope.launch {
            model.playlist.collect { playlist ->
                model.fetchAllSongs(this@PlaylistDetailActivity)
                supportActionBar!!.title = playlist.name
                if (!playlist.isVirtual() &&
                    !PlaylistLoader2.checkExistence(this@PlaylistDetailActivity, playlist.id)
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
                    model.searchSongs(word)
                }
            }
        }
        lifecycleScope.launch {
            model.searchResults.collect { songs ->
                if (model.currentMode.value == UIMode.Search) {
                    adapter.dataset = songs
                    binding.empty.visibility = if (songs.isEmpty()) VISIBLE else GONE
                }
            }
        }
    }

    override fun createContentView(): View = wrapSlidingMusicPanel(binding.root)

    private fun setUpToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
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
        adapter = PlaylistSongDisplayAdapter(this, model)
        // DragDropAdapter
        binding.recyclerView.also { recyclerView ->
            recyclerViewDragDropManager = RecyclerViewDragDropManager()
            recyclerViewDragDropManager!!.attachRecyclerView(recyclerView)
            wrappedAdapter = recyclerViewDragDropManager!!.createWrappedAdapter(adapter)

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

    private fun updateSearchBarVisibility(visibility: Boolean) {
        with(binding) {
            searchBar.visibility = if (visibility) VISIBLE else GONE
            searchBox.editQuery.setText(if (visibility) model.keyword.value else "")
            updateRecyclerviewPadding(if (visibility) 0 else searchBar.height)
        }
    }

    private fun updateDashboard(playlist: Playlist, songs: List<Song>) {
        // text
        with(binding) {
            nameText.text = playlist.name
            songCountText.text = songs.size.toString()
            durationText.text = getReadableDurationString(songs.totalDuration())
            pathText.text = playlist.location.text(this@PlaylistDetailActivity)
        }
    }

    private fun setupMenu(menu: Menu) {
        playlistDetailToolbar(menu, this, model, iconColor = primaryTextColor(primaryColor()))
        setToolbarColor(binding.toolbar, primaryColor())
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
            if (model.currentMode.value != UIMode.Editor) {
                adapter.dataset = emptyList()
                model.refreshPlaylist(this@PlaylistDetailActivity)
            }
        }
    }

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
