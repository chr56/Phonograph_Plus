/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.ui.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.afollestad.materialdialogs.utils.MDUtil.getStringArray
import com.bumptech.glide.Glide
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import lib.phonograph.cab.*
import player.phonograph.App
import player.phonograph.PlaylistType
import player.phonograph.R
import player.phonograph.adapter.display.Dashboard
import player.phonograph.adapter.display.ListSheetAdapter
import player.phonograph.databinding.ActivityPlaylistDetailBinding
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.helper.menu.PlaylistMenuHelper.handleMenuClick
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.misc.SAFCallbackHandlerActivity
import player.phonograph.misc.SafLauncher
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.GeneratedPlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PhonographColorUtil
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer

class PlaylistDetailActivity : AbsSlidingMusicPanelActivity(), SAFCallbackHandlerActivity, MultiSelectionCabProvider {

    private lateinit var binding: ActivityPlaylistDetailBinding

    private val model: PlaylistModel by viewModels()

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
        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)

        setDrawUnderStatusbar()

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        Themer.setActivityToolbarColorAuto(this, binding.toolbar)

        model.playlist.observe(this) {
            model.fetchPlaylist(this, playlistCallBack)
        }
        model.playlist.value = intent.extras!!.getParcelable(EXTRA_PLAYLIST)!!

        safLauncher = SafLauncher(activityResultRegistry)
        lifecycle.addObserver(safLauncher)

        setUpToolbar()
        setUpRecyclerView()
    }

    private val primaryColor: Int get() = ThemeColor.primaryColor(this)
    private val accentColor: Int get() = ThemeColor.accentColor(this)

    override fun createContentView(): View = wrapSlidingMusicPanel(binding.root)

    private fun setUpToolbar() {
        binding.toolbar.setBackgroundColor(primaryColor)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    private fun setUpRecyclerView() {
        binding.recyclerView.setUpFastScrollRecyclerViewColor(this, accentColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        val playlist: Playlist = model.playlist.value!! // todo
        // Init adapter
        adapter = ListSheetAdapter(
            this, this,
            ArrayList(),
            Dashboard(playlist.name),
            R.layout.item_list,
            R.layout.item_header_playlist,
        ) { loadImageImpl = loadImage }

        if (playlist is SmartPlaylist) {
            binding.recyclerView.adapter = adapter
        } else {
            recyclerViewDragDropManager = RecyclerViewDragDropManager()

            adapter.dashboard = Dashboard(playlist.name, path = PlaylistsUtil.getPlaylistPath(this, playlist as FilePlaylist))
            wrappedAdapter = recyclerViewDragDropManager!!.createWrappedAdapter(adapter)

            binding.recyclerView.itemAnimator = RefactoredDefaultItemAnimator()
            binding.recyclerView.adapter = wrappedAdapter

            recyclerViewDragDropManager!!.attachRecyclerView(binding.recyclerView)
        }

        model.isRecyclerViewReady = true
    }

    private val playlistCallBack: PlaylistCallback
        get() = { playlist: Playlist, songs: List<Song> ->
            adapter.dataset = songs
            adapter.dashboard.name = playlist.name
            adapter.updateDashboardText()
            binding.empty.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
            supportActionBar!!.title = playlist.name
            if (playlist !is SmartPlaylist && !PlaylistsUtil.doesPlaylistExist(this, playlist.id)) {
                // File Playlist was deleted
                finish()
            }
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val playlist: Playlist = model.playlist.value!! // todo
        menuInflater.inflate(
            if (playlist is SmartPlaylist) R.menu.menu_smart_playlist_detail else R.menu.menu_playlist_detail, menu
        )
        if (playlist.type == PlaylistType.LAST_ADDED)
            menu.add(Menu.NONE, R.id.action_setting_last_added_interval, Menu.NONE, R.string.pref_title_last_added_interval)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_shuffle_playlist -> {
                MusicPlayerRemote.openAndShuffleQueue(adapter.dataset, true)
                true
            }
            R.id.action_edit_playlist -> {
                startActivityForResult(
                    Intent(this, PlaylistEditorActivity::class.java).apply {
                        putExtra(EXTRA_PLAYLIST, model.playlist.value!!)
                    },
                    EDIT_PLAYLIST
                )
                true
            }
            R.id.action_refresh -> {
                val playlist: Playlist = model.playlist.value!! // todo
                adapter.dataset = emptyList()
                if (playlist is GeneratedPlaylist) {
                    (playlist as GeneratedPlaylist).refresh(this)
                }
                model.playlist.postValue(playlist)
                true
            }
            R.id.action_setting_last_added_interval -> {
                val prefValue = App.instance.getStringArray(R.array.pref_playlists_last_added_interval_values)
                val currentChoice = prefValue.indexOf(Setting.instance.lastAddedCutoffPref)
                MaterialDialog(this)
                    .listItemsSingleChoice(
                        res = R.array.pref_playlists_last_added_interval_titles,
                        initialSelection = currentChoice.let { if (it == -1) 0 else it },
                        checkedColor = accentColor
                    ) { dialog, index, _ ->
                        runCatching {
                            Setting.instance.lastAddedCutoffPref = prefValue[index]
                        }.apply {
                            if (isSuccess) {
                                model.playlist.postValue(model.playlist.value)
                            }
                        }
                        dialog.dismiss()
                    }
                    .title(R.string.pref_title_last_added_interval)
                    .show()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> handleMenuClick(this, model.playlist.value!!, item)
        }
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
        model.playlist.postValue(PlaylistsUtil.getPlaylist(this, model.playlist.value!!.id))
    }

    override fun onPause() {
        recyclerViewDragDropManager?.cancelDrag()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        multiSelectionCab?.destroy()
        multiSelectionCab = null
        recyclerViewDragDropManager?.let {
            it.release()
            recyclerViewDragDropManager = null
        }
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.adapter = null
        wrappedAdapter?.let {
            WrapperAdapterUtils.releaseAll(it)
            wrappedAdapter = null
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
        destroyCallback: DestroyCallback?,
    ): MultiSelectionCab {
        val cfg: CabCfg = {
            val primaryColor = ThemeColor.primaryColor(this@PlaylistDetailActivity)
            val textColor = Color.WHITE

            backgroundColor = PhonographColorUtil.shiftBackgroundColorForLightText(primaryColor)
            titleTextColor = textColor

            closeDrawable = getTintedDrawable(R.drawable.ic_close_white_24dp, textColor)!!

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
