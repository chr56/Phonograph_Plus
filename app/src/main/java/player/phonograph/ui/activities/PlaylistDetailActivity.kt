/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import kotlinx.coroutines.*
import lib.phonograph.cab.*
import player.phonograph.R
import player.phonograph.adapter.display.Dashboard
import player.phonograph.adapter.display.ListSheetAdapter
import player.phonograph.databinding.ActivityPlaylistDetailBinding
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.helper.menu.PlaylistMenuHelper.handleMenuClick
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.*
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer

class PlaylistDetailActivity : AbsSlidingMusicPanelActivity(), SAFCallbackHandlerActivity, MultiSelectionCabProvider {

    private var _binding: ActivityPlaylistDetailBinding? = null
    private val binding: ActivityPlaylistDetailBinding get() = _binding!!

    private lateinit var playlist: Playlist // init in OnCreate()

    private lateinit var adapter: ListSheetAdapter<Song> // init in OnCreate() -> setUpRecyclerView()
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null

    // for saf callback
    private lateinit var safLauncher: SafLauncher
    override fun getSafLauncher(): SafLauncher = safLauncher

    /* ********************
     *
     *  First Initialization
     *
     * ********************/

    override fun onCreate(savedInstanceState: Bundle?) {
        _binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)

        setDrawUnderStatusbar()

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        Themer.setActivityToolbarColorAuto(this, binding.toolbar)

        playlist = intent.extras!!.getParcelable(EXTRA_PLAYLIST)!!
        loadSongs()

        safLauncher = SafLauncher(activityResultRegistry)
        lifecycle.addObserver(safLauncher)

        setUpToolbar()
        setUpRecyclerView()
    }
    override fun createContentView(): View {
        return wrapSlidingMusicPanel(binding.root)
    }

    private fun setUpToolbar() {
        binding.toolbar.setBackgroundColor(ThemeColor.primaryColor(this))
        setSupportActionBar(binding.toolbar)
        setToolbarTitle(playlist.name)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    private var isRecyclerViewReady = false
    private fun setUpRecyclerView() {
        ViewUtil.setUpFastScrollRecyclerViewColor(
            this, binding.recyclerView, ThemeColor.accentColor(this)
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // Init (song)adapter
        if (playlist is SmartPlaylist) {
            adapter = ListSheetAdapter(
                this, this,
                ArrayList(),
                Dashboard(playlist.name),
                R.layout.item_list,
                R.layout.item_header_playlist,
            ) { loadImageImpl = loadImage }
            binding.recyclerView.adapter = adapter
        } else {
            recyclerViewDragDropManager = RecyclerViewDragDropManager()
            val animator: GeneralItemAnimator = RefactoredDefaultItemAnimator()

            adapter = ListSheetAdapter(
                this, this,
                ArrayList(),
                Dashboard(playlist.name, path = PlaylistsUtil.getPlaylistPath(this, playlist as FilePlaylist)),
                R.layout.item_list,
                R.layout.item_header_playlist,
            ) { loadImageImpl = loadImage }
            wrappedAdapter = recyclerViewDragDropManager!!.createWrappedAdapter(adapter)
            binding.recyclerView.adapter = wrappedAdapter
            binding.recyclerView.itemAnimator = animator
            recyclerViewDragDropManager!!.attachRecyclerView(binding.recyclerView)
        }

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
        isRecyclerViewReady = true
    }

    private fun checkIsEmpty() {
        binding.empty.visibility = if (adapter.dataset.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun setToolbarTitle(title: String) {
        supportActionBar!!.title = title
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(
            if (playlist is SmartPlaylist) R.menu.menu_smart_playlist_detail else R.menu.menu_playlist_detail, menu
        )
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_shuffle_playlist -> {
                MusicPlayerRemote.openAndShuffleQueue(adapter.dataset, true)
                return true
            }
            R.id.action_edit_playlist -> {
                startActivityForResult(
                    Intent(this, PlaylistEditorActivity::class.java).apply {
                        putExtra(EXTRA_PLAYLIST, playlist)
                    },
                    EDIT_PLAYLIST
                )
                return true
            }
            R.id.action_refresh -> {
                onMediaStoreChanged()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return handleMenuClick(this, playlist, item)
    }

    override fun onBackPressed() {
        if (multiSelectionCab != null && multiSelectionCab!!.status == CabStatus.STATUS_ACTIVE) {
            dismissCab()
            return
        } else if (multiSelectionCab != null) {
            multiSelectionCab!!.destroy()
            multiSelectionCab = null
        }
        binding.recyclerView.stopScroll()
        super.onBackPressed()
    }

    /* *******************
     *
     *    States Changed
     *
     * *******************/

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        loadSongs()
        if (playlist !is SmartPlaylist) {
            // Playlist deleted
            if (!PlaylistsUtil.doesPlaylistExist(this, playlist.id)) {
                finish()
                return
            }

            // Playlist renamed
            val playlistName = PlaylistsUtil.getNameForPlaylist(this, playlist.id)
            if (playlistName != playlist.name) {
                playlist = PlaylistsUtil.getPlaylist(this, playlist.id)
                setToolbarTitle(playlist.name)
                adapter.dashboard.name = playlist.name
                adapter.updateDashboardText()
            }
        }
    }

    override fun onPause() {
        recyclerViewDragDropManager?.cancelDrag()
        super.onPause()
    }

    override fun onDestroy() {
        try { loaderCoroutineScope.coroutineContext[Job]?.cancel() } catch (e: java.lang.Exception) { Log.i("BackgroundCoroutineScope", e.message.orEmpty()) }
        super.onDestroy()
        multiSelectionCab?.destroy()
        multiSelectionCab = null
        recyclerViewDragDropManager?.let {
            it.release()
            recyclerViewDragDropManager = null
        }
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = null
        wrappedAdapter?. let {
            WrapperAdapterUtils.releaseAll(it)
            wrappedAdapter = null
        }
        _binding = null
    }

    /* *******************
     *
     *   Load Playlist
     *
     * *******************/

    private val loaderCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    fun loadSongs() {
        fetchPlaylist(playlist)
    }

    private fun fetchPlaylist(list: Playlist) {
        loaderCoroutineScope.launch {
            val songs = list.getSongs(this@PlaylistDetailActivity)

            while (!isRecyclerViewReady) yield()
            withContext(Dispatchers.Main) {
                if (isRecyclerViewReady) {
                    adapter.dataset = songs
                }
            }
        }
    }

    /* *******************
     *
     *     cabCallBack
     *
     * *******************/

    private var multiSelectionCab: MultiSelectionCab? = null

    override fun deployCab(
        menuRes: Int,
        initCallback: InitCallback?,
        showCallback: ShowCallback?,
        selectCallback: SelectCallback?,
        hideCallback: HideCallback?,
        destroyCallback: lib.phonograph.cab.DestroyCallback?
    ): MultiSelectionCab {
        val cfg: CabCfg = {
            val primaryColor = ThemeColor.primaryColor(this@PlaylistDetailActivity)
            val textColor = Color.WHITE

            backgroundColor = PhonographColorUtil.shiftBackgroundColorForLightText(primaryColor)
            titleTextColor = textColor

            closeDrawable = AppCompatResources.getDrawable(this@PlaylistDetailActivity, R.drawable.ic_close_white_24dp)!!.also {
                it.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(textColor, BlendModeCompat.SRC_IN)
            }

            this.menuRes = menuRes

            onInit(initCallback)
            onShow(showCallback)
            onSelection(selectCallback)
            onHide(hideCallback)
            onClose { dismissCab() }
            onDestroy(destroyCallback)
        }

        if (multiSelectionCab == null) multiSelectionCab =
            createMultiSelectionCab(this, R.id.cab_stub, R.id.multi_selection_cab, cfg)
        else {
            multiSelectionCab!!.applyCfg = cfg
            multiSelectionCab!!.refresh()
        }

        return multiSelectionCab!!
    }

    override fun getCab(): MultiSelectionCab? = multiSelectionCab

    override fun showCab() {
        multiSelectionCab?.let { cab ->
            binding.toolbar.visibility = View.INVISIBLE
            cab.refresh()
            cab.show()
        }
    }
    override fun dismissCab() {
        multiSelectionCab?.hide()
        binding.toolbar.visibility = View.VISIBLE
    }

    /* *******************
     *   companion object
     * *******************/

    companion object {
        private const val TAG = "PlaylistDetail"
        const val EXTRA_PLAYLIST = "extra_playlist"
        const val EDIT_PLAYLIST: Int = 100
    }

    private val loadImage: (ImageView, Song) -> Unit =
        { image: ImageView, song: Song ->
            SongGlideRequest.Builder.from(Glide.with(this), song)
                .checkIgnoreMediaStore(this)
                .generatePalette(this).build()
                .into(object : PhonographColoredTarget(image) {
                    override fun onLoadCleared(placeholder: Drawable?) {
                        super.onLoadCleared(placeholder)
//                            setPaletteColors(defaultFooterColor, holder)
                    }

                    override fun onColorReady(color: Int) {
//                            if (usePalette) setPaletteColors(color, holder)
//                            else setPaletteColors(defaultFooterColor, holder)
                    }
                })
        }
}
