package player.phonograph.ui.modules.album

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
import player.phonograph.databinding.ActivityAlbumDetailBinding
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Album
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.model.ui.PaletteColorProvider
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.SongBasicDisplayPresenter
import player.phonograph.ui.goToArtistDetail
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.ui.resource.Durations
import player.phonograph.ui.resource.buildInfoString
import player.phonograph.ui.resource.readableYear
import player.phonograph.ui.resource.songCountString
import player.phonograph.ui.theme.SystemBarsControllerDelegate
import player.phonograph.ui.theme.ThemeSettingsDelegate.accentColor
import player.phonograph.ui.theme.ThemeSettingsDelegate.primaryColor
import player.phonograph.ui.theme.getTintedDrawable
import player.phonograph.ui.theme.secondaryTextColorOn
import player.phonograph.ui.theme.setUpFastScrollRecyclerViewColor
import player.phonograph.ui.theme.textColorOn
import player.phonograph.ui.util.BottomViewWindowInsetsController
import player.phonograph.ui.util.applyControllableWindowInsetsAsBottomView
import player.phonograph.ui.util.menuProvider
import player.phonograph.ui.util.observe
import util.theme.color.darkenColor
import util.theme.view.menu.tintOverflowButtonColor
import util.theme.view.menu.tintOverflowMenuItems
import util.theme.view.menu.tintToolbarMenuActionIcons
import util.theme.view.toolbar.setToolbarColor
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumDetailActivity : AbsSlidingMusicPanelActivity(), PaletteColorProvider,
                            ICreateFileStorageAccessible, IOpenFileStorageAccessible, IOpenDirStorageAccessible {

    private lateinit var viewBinding: ActivityAlbumDetailBinding
    private val viewModel: AlbumDetailActivityViewModel by viewModel { parametersOf(parseIntent(intent)) }

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()

    private lateinit var songAdapter: DisplayAdapter<Song>
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun createContentView(): View = wrapSlidingMusicPanel(viewBinding.root)

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.loadDataSet(this)

        registerActivityResultLauncherDelegate(
            createFileStorageAccessDelegate,
            openFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
        )

        viewBinding = ActivityAlbumDetailBinding.inflate(layoutInflater) // must call before super due to `createContentView()`

        super.onCreate(savedInstanceState)

        setUpToolbar()
        setUpMainContent()

        observeData()

        lifecycle.addObserver(MediaStoreListener())

        // back-press
        onBackPressedDispatcher.addCallback {
            remove()
            viewBinding.recyclerView.stopScroll()
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setUpToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewBinding.toolbar.setNavigationOnClickListener {
            if (!isTaskRoot) onBackPressedDispatcher.onBackPressed()
        }
        viewBinding.toolbar.title = getString(R.string.label_album)
        addMenuProvider(menuProvider(this::setupMenu))
        setToolbarColor(viewBinding.toolbar, primaryColor())
    }

    private fun setupMenu(menu: Menu) {
        val iconColor = textColorOn(this, panelViewModel.activityColor.value)
        inflateAlbumDetailMenu(menu, this, viewModel.album.value, iconColor)
        tintToolbarMenuActionIcons(menu, iconColor)
        tintOverflowButtonColor(this, iconColor)
        tintOverflowMenuItems(viewBinding.toolbar, accentColor())
    }

    private lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController
    private fun setUpMainContent() {

        songAdapter = DisplayAdapter(this, AlbumDetailActivityDisplayAdapters)
        linearLayoutManager = LinearLayoutManager(this@AlbumDetailActivity)
        with(viewBinding.recyclerView) {
            layoutManager = linearLayoutManager
            adapter = songAdapter
        }

        viewBinding.artistText.setOnClickListener {
            lifecycleScope.launch {
                goToArtistDetail(this@AlbumDetailActivity, viewModel.album.value)
            }
        }

        // Paddings
        viewBinding.innerAppBar.addOnOffsetChangedListener { _, verticalOffset ->
            with(viewBinding.recyclerView) {
                setPadding(paddingLeft, viewBinding.innerAppBar.totalScrollRange + verticalOffset, paddingRight, paddingBottom)
            }
        }
        // WindowInsets
        bottomViewWindowInsetsController = viewBinding.recyclerView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isMiniPlayerHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }
    }

    private fun observeData() {
        observe(viewModel.album) { album -> if (album.id >= 0) updateAlbumsInfo(album) }
        observe(viewModel.songs) { songs -> songAdapter.dataset = songs }
        observe(viewModel.paletteColor) { color -> updateColors(color) }
    }

    private fun updateColors(color: Int) {
        val textColor = textColorOn(this, color)
        val secondaryTextColor = secondaryTextColorOn(this, color)

        setToolbarColor(viewBinding.toolbar, color)
        viewBinding.toolbar.setTitleTextColor(textColor)

        val statusBarColor = darkenColor(color)
        val navigationBarColor = if (panelViewModel.isMiniPlayerHidden.value) Color.TRANSPARENT else color
        SystemBarsControllerDelegate.updateSystemBarsColor(this, statusBarColor, navigationBarColor)
        panelViewModel.updateActivityColor(color)

        viewBinding.recyclerView.setUpFastScrollRecyclerViewColor(this, color)
        viewBinding.header.setBackgroundColor(color)

        val artistIcon = getTintedDrawable(R.drawable.ic_person_white_24dp, secondaryTextColor)
        viewBinding.artistText.setCompoundDrawablesWithIntrinsicBounds(artistIcon, null, null, null)
        viewBinding.artistText.setTextColor(textColor)
        viewBinding.artistText.compoundDrawablePadding = 16

        val songCountIcon = getTintedDrawable(R.drawable.ic_music_note_white_24dp, secondaryTextColor)
        viewBinding.songCountText.setCompoundDrawablesWithIntrinsicBounds(songCountIcon, null, null, null)
        viewBinding.songCountText.setTextColor(secondaryTextColor)
        viewBinding.songCountText.compoundDrawablePadding = 16

        val durationIcon = getTintedDrawable(R.drawable.ic_timer_white_24dp, secondaryTextColor)
        viewBinding.durationText.setCompoundDrawablesWithIntrinsicBounds(durationIcon, null, null, null)
        viewBinding.durationText.setTextColor(secondaryTextColor)
        viewBinding.durationText.compoundDrawablePadding = 16

        val albumYearIcon = getTintedDrawable(R.drawable.ic_event_white_24dp, secondaryTextColor)
        viewBinding.albumYearText.setCompoundDrawablesWithIntrinsicBounds(albumYearIcon, null, null, null)
        viewBinding.albumYearText.setTextColor(secondaryTextColor)
        viewBinding.albumYearText.compoundDrawablePadding = 16
    }

    private suspend fun updateAlbumsInfo(album: Album) {
        viewModel.loadAlbumImage(this, album, viewBinding.image)
        viewBinding.toolbar.title = album.title
        viewBinding.artistText.text = album.artistName
        viewBinding.songCountText.text = songCountString(this, album.songCount)
        val songs = withContext(Dispatchers.IO) { Songs.album(this@AlbumDetailActivity, album.id) }
        viewBinding.durationText.text = Durations.short(
            songs.fold(0L) { acc: Long, song: Song -> acc + song.duration }
        )
        viewBinding.albumYearText.text = readableYear(album.year)
    }

    private inner class MediaStoreListener : EventHub.LifeCycleEventReceiver(this, EventHub.EVENT_MUSIC_LIBRARY_CHANGED) {
        override fun onEventReceived(context: Context, intent: Intent) {
            viewModel.loadDataSet(this@AlbumDetailActivity)
        }
    }

    override val paletteColor: StateFlow<Int> get() = viewModel.paletteColor

    companion object {

        private const val EXTRA_ALBUM_ID = "extra_album_id"
        fun launchIntent(from: Context, albumId: Long): Intent =
            Intent(from, AlbumDetailActivity::class.java).apply {
                putExtra(EXTRA_ALBUM_ID, albumId)
            }

        private fun parseIntent(intent: Intent): Long = intent.extras?.getLong(EXTRA_ALBUM_ID) ?: -1
    }


    object AlbumDetailActivityDisplayAdapters : SongBasicDisplayPresenter(SortMode(SortRef.ID)) {

        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST

        override val usePalette: Boolean get() = false

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_TEXT

        override fun getRelativeOrdinalText(item: Song): String = trackNumber(item)

        override fun getSortOrderReference(item: Song, sortMode: SortMode): String = trackNumber(item)

        override fun getNonSortOrderReference(item: Song): String = trackNumber(item)

        override fun getDescription(context: Context, item: Song): CharSequence =
            buildInfoString(Durations.short(item.duration), item.artistName)

        private fun trackNumber(item: Song): String {
            // iTunes uses for example 1002 for track 2 CD1 or 3011 for track 11 CD3.
            val num = item.trackNumber % 1000
            return if (num > 0) num.toString() else "-"
        }
    }
}
