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
import player.phonograph.ui.NavigationUtil.goToArtist
import player.phonograph.ui.actions.DetailToolbarMenuProviders
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.SongBasicDisplayPresenter
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.util.observe
import player.phonograph.util.text.buildInfoString
import player.phonograph.util.text.readableDuration
import player.phonograph.util.text.readableYear
import player.phonograph.util.text.songCountString
import player.phonograph.util.text.totalDuration
import player.phonograph.util.theme.ThemeSettingsDelegate.primaryColor
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.updateSystemBarsColor
import player.phonograph.util.ui.BottomViewWindowInsetsController
import player.phonograph.util.ui.applyControllableWindowInsetsAsBottomView
import player.phonograph.util.ui.menuProvider
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import util.theme.view.menu.tintOverflowButtonColor
import util.theme.view.menu.tintToolbarMenuActionIcons
import util.theme.view.toolbar.setToolbarColor
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

/**
 * Be careful when changing things in this Activity!
 */
class AlbumDetailActivity : AbsSlidingMusicPanelActivity(), PaletteColorProvider,
                            ICreateFileStorageAccessible, IOpenFileStorageAccessible, IOpenDirStorageAccessible {

    private lateinit var viewBinding: ActivityAlbumDetailBinding
    private val viewModel: AlbumDetailActivityViewModel by viewModel { parametersOf(parseIntent(intent)) }

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()

    private lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController

    private lateinit var songAdapter: DisplayAdapter<Song>
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.loadDataSet(this)

        viewBinding = ActivityAlbumDetailBinding.inflate(layoutInflater)


        registerActivityResultLauncherDelegate(
            createFileStorageAccessDelegate,
            openFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
        )

        super.onCreate(savedInstanceState)

        // activity
        setUpToolbar()

        // content
        setUpViews()

        // MediaStore
        lifecycle.addObserver(MediaStoreListener())

        // Observer
        observe(viewModel.album) { album ->
            if (album.id >= 0) {
                updateAlbumsInfo(album)
                viewModel.loadAlbumImage(this@AlbumDetailActivity, album, viewBinding.image)
            }
        }
        observe(viewModel.songs) { songs ->
            songAdapter.dataset = songs
        }
        observe(viewModel.paletteColor) { color ->
            updateColors(color)
        }

        // back-press
        onBackPressedDispatcher.addCallback {
            remove()
            viewBinding.recyclerView.stopScroll()
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun createContentView(): View = wrapSlidingMusicPanel(viewBinding.root)

    private fun setUpToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setToolbarColor(viewBinding.toolbar, primaryColor())
    }

    private fun setUpViews() {
        // Adapter
        songAdapter = DisplayAdapter(this, AlbumSongDisplayPresenter)
        linearLayoutManager = LinearLayoutManager(this@AlbumDetailActivity)
        with(viewBinding.recyclerView) {
            layoutManager = linearLayoutManager
            adapter = songAdapter
        }
        // Links
        viewBinding.artistText.setOnClickListener {
            val album = viewModel.album.value
            lifecycleScope.launch {
                goToArtist(this@AlbumDetailActivity, album, null)
            }
        }
        // AppBar
        viewBinding.innerAppBar.addOnOffsetChangedListener { _, verticalOffset ->
            viewBinding.recyclerView.setPaddingTop(viewBinding.innerAppBar.totalScrollRange + verticalOffset)
        }
        // WindowInsets
        bottomViewWindowInsetsController = viewBinding.recyclerView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isPanelHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }
    }

    private fun RecyclerView.setPaddingTop(top: Int) = setPadding(paddingLeft, top, paddingRight, paddingBottom)

    private fun updateColors(color: Int) {
        viewBinding.recyclerView.setUpFastScrollRecyclerViewColor(this, color)
        viewBinding.header.setBackgroundColor(color)


        viewBinding.toolbar.setBackgroundColor(color)
        setSupportActionBar(viewBinding.toolbar) // needed to auto readjust the toolbar content color
        updateSystemBarsColor(color, Color.TRANSPARENT)
        setToolbarColor(viewBinding.toolbar, color)

        val secondaryTextColor = secondaryTextColor(color)

        val artistIcon = getTintedDrawable(R.drawable.ic_person_white_24dp, secondaryTextColor)!!
        viewBinding.artistText.setCompoundDrawablesWithIntrinsicBounds(artistIcon, null, null, null)
        viewBinding.artistText.setTextColor(primaryTextColor(color))
        viewBinding.artistText.compoundDrawablePadding = 16

        val songCountIcon = getTintedDrawable(R.drawable.ic_music_note_white_24dp, secondaryTextColor)!!
        viewBinding.songCountText.setTextColor(secondaryTextColor)
        viewBinding.songCountText.setCompoundDrawablesWithIntrinsicBounds(songCountIcon, null, null, null)
        viewBinding.songCountText.compoundDrawablePadding = 16

        val durationIcon = getTintedDrawable(R.drawable.ic_timer_white_24dp, secondaryTextColor)!!
        viewBinding.durationText.setTextColor(secondaryTextColor)
        viewBinding.durationText.setCompoundDrawablesWithIntrinsicBounds(durationIcon, null, null, null)
        viewBinding.durationText.compoundDrawablePadding = 16

        val albumYearIcon = getTintedDrawable(R.drawable.ic_event_white_24dp, secondaryTextColor)!!
        viewBinding.albumYearText.setTextColor(secondaryTextColor)
        viewBinding.albumYearText.setCompoundDrawablesWithIntrinsicBounds(albumYearIcon, null, null, null)
        viewBinding.albumYearText.compoundDrawablePadding = 16

        panelViewModel.updateActivityColor(color)
    }

    override val paletteColor: StateFlow<Int> get() = viewModel.paletteColor

    private suspend fun updateAlbumsInfo(album: Album) {
        viewBinding.toolbar.title = album.title
        viewBinding.artistText.text = album.artistName
        viewBinding.songCountText.text = songCountString(this, album.songCount)
        val songs = withContext(Dispatchers.IO) {
            Songs.album(this@AlbumDetailActivity, album.id)
        }
        viewBinding.durationText.text = readableDuration(totalDuration(songs))
        viewBinding.albumYearText.text = readableYear(album.year)
    }

    private fun setupMenu(menu: Menu) {
        val iconColor = primaryTextColor(panelViewModel.activityColor.value)
        DetailToolbarMenuProviders.AlbumToolbarMenuProvider.inflateMenu(menu, this, viewModel.album.value, iconColor)
        tintToolbarMenuActionIcons(menu, iconColor)
        tintOverflowButtonColor(this, iconColor)
    }

    private inner class MediaStoreListener :
            EventHub.LifeCycleEventReceiver(this, EventHub.EVENT_MUSIC_LIBRARY_CHANGED) {
        override fun onEventReceived(context: Context, intent: Intent) {
            viewModel.loadDataSet(this@AlbumDetailActivity)
        }
    }

    companion object {

        private const val EXTRA_ALBUM_ID = "extra_album_id"
        fun launchIntent(from: Context, albumId: Long): Intent =
            Intent(from, AlbumDetailActivity::class.java).apply {
                putExtra(EXTRA_ALBUM_ID, albumId)
            }

        private fun parseIntent(intent: Intent): Long = intent.extras?.getLong(EXTRA_ALBUM_ID) ?: -1
    }

    object AlbumSongDisplayPresenter : SongBasicDisplayPresenter(SortMode(SortRef.ID)) {

        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST

        override val usePalette: Boolean get() = false

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_TEXT

        override fun getRelativeOrdinalText(item: Song): String = trackNumber(item)

        override fun getSortOrderReference(item: Song, sortMode: SortMode): String = trackNumber(item)

        override fun getNonSortOrderReference(item: Song): String = trackNumber(item)

        override fun getDescription(context: Context, item: Song): CharSequence =
            buildInfoString(readableDuration(item.duration), item.artistName)

        private fun trackNumber(item: Song): String {
            // iTunes uses for example 1002 for track 2 CD1 or 3011 for track 11 CD3.
            val num = item.trackNumber % 1000
            return if (num > 0) num.toString() else "-"
        }
    }

}
