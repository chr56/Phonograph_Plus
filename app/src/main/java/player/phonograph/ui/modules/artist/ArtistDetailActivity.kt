package player.phonograph.ui.modules.artist

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
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
import player.phonograph.databinding.ActivityArtistDetailBinding
import player.phonograph.foundation.content.GetContentDelegate
import player.phonograph.foundation.content.IGetContentRequester
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.model.ui.PaletteColorProvider
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.adapter.AlbumBasicDisplayPresenter
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.ui.adapter.SongBasicDisplayPresenter
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.ui.resource.Durations
import player.phonograph.ui.resource.Layouts
import player.phonograph.ui.resource.albumCountString
import player.phonograph.ui.resource.buildInfoString
import player.phonograph.ui.resource.readableYear
import player.phonograph.ui.resource.songCountString
import player.phonograph.ui.theme.SystemBarsControllerDelegate
import player.phonograph.ui.theme.ThemeSettingsDelegate.accentColor
import player.phonograph.ui.theme.ThemeSettingsDelegate.primaryColor
import player.phonograph.ui.theme.getTintedDrawable
import player.phonograph.ui.theme.secondaryTextColorOn
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
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class ArtistDetailActivity : AbsSlidingMusicPanelActivity(), PaletteColorProvider, IGetContentRequester,
                             ICreateFileStorageAccessible, IOpenFileStorageAccessible, IOpenDirStorageAccessible {

    private lateinit var viewBinding: ActivityArtistDetailBinding
    private val viewModel: ArtistDetailActivityViewModel by viewModel { parametersOf(parseIntent(intent)) }

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()
    override val getContentDelegate: GetContentDelegate = GetContentDelegate()

    private lateinit var albumAdapter: ArtistAlbumDisplayAdapter
    private lateinit var songAdapter: DisplayAdapter<Song>

    override fun createContentView(): View = wrapSlidingMusicPanel(viewBinding.root)

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.load(this)

        registerActivityResultLauncherDelegate(
            createFileStorageAccessDelegate,
            openFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
            getContentDelegate,
        )

        viewBinding = ActivityArtistDetailBinding.inflate(layoutInflater) // must call before super due to `createContentView()`

        super.onCreate(savedInstanceState)

        setUpToolbar()
        setUpMainContent()

        observeData()

        lifecycle.addObserver(MediaStoreListener())

        // back-press
        onBackPressedDispatcher.addCallback {
            remove()
            viewBinding.albumRecycleView.stopScroll()
            viewBinding.songsRecycleView.stopScroll()
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setUpToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewBinding.toolbar.setNavigationOnClickListener {
            if (!isTaskRoot) onBackPressedDispatcher.onBackPressed()
        }
        viewBinding.toolbar.title = getString(R.string.label_artist)
        addMenuProvider(menuProvider(this::setupMenu))
        setToolbarColor(viewBinding.toolbar, primaryColor())
    }

    private fun setupMenu(menu: Menu) {
        val iconColor = textColorOn(this, panelViewModel.activityColor.value)
        inflateArtistDetailMenu(menu, this, viewModel.artist.value ?: Artist(), iconColor)
        attach(menu) {
            menuItem(title = getString(R.string.label_colored_footers)) {
                checkable = true
                checked = viewModel.usePaletteColor.value
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    it.isChecked = !it.isChecked
                    viewModel.updateUsePaletteColor(it.isChecked)
                    true
                }
            }
        }
        tintToolbarMenuActionIcons(menu, iconColor)
        tintOverflowButtonColor(this, iconColor)
        tintOverflowMenuItems(viewBinding.toolbar, accentColor())
    }

    private lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController
    private fun setUpMainContent() {

        songAdapter = DisplayAdapter(this, ArtistSongDisplayPresenter)
        with(viewBinding.songsRecycleView) {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(this@ArtistDetailActivity, VERTICAL, false)
        }

        albumAdapter = ArtistAlbumDisplayAdapter(this, ArtistAlbumDisplayPresenter(false))
        with(viewBinding.albumRecycleView) {
            adapter = albumAdapter
            layoutManager = LinearLayoutManager(this@ArtistDetailActivity, HORIZONTAL, false)
        }

        // Paddings
        viewBinding.innerAppBar.addOnOffsetChangedListener { _, verticalOffset ->
            with(viewBinding.mainContent) {
                setPadding(paddingLeft, verticalOffset, paddingRight, paddingBottom)
            }
        }
        // WindowInsets
        bottomViewWindowInsetsController = viewBinding.songsRecycleView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isPanelHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }
    }

    private fun observeData() {
        observe(viewModel.artist) { artist -> updateArtistInfo(artist ?: Artist()) }
        observe(viewModel.albums) { albums -> albumAdapter.dataset = albums ?: emptyList() }
        observe(viewModel.songs) { songs -> songAdapter.dataset = songs ?: emptyList() }
        observe(viewModel.paletteColor) { color -> updateColors(color) }
        observe(viewModel.usePaletteColor) {
            albumAdapter.presenter = ArtistAlbumDisplayPresenter(it)
            val dataset = albumAdapter.dataset
            synchronized(albumAdapter) {
                albumAdapter.dataset = emptyList()
                albumAdapter.dataset = dataset
            }
        }
    }

    private fun updateColors(color: Int) {
        val textColor = textColorOn(this, color)
        val secondaryTextColor = secondaryTextColorOn(this, color)

        setToolbarColor(viewBinding.toolbar, color)
        viewBinding.toolbar.setTitleTextColor(textColor)

        val statusBarColor = darkenColor(color)
        val navigationBarColor = if (panelViewModel.isPanelHidden.value) Color.TRANSPARENT else color
        SystemBarsControllerDelegate.updateSystemBarsColor(this, statusBarColor, navigationBarColor)
        panelViewModel.updateActivityColor(color)

        viewBinding.header.setBackgroundColor(color)
        viewBinding.durationIcon.setImageDrawable(
            getTintedDrawable(R.drawable.ic_timer_white_24dp, secondaryTextColor)
        )
        viewBinding.songCountIcon.setImageDrawable(
            getTintedDrawable(R.drawable.ic_music_note_white_24dp, secondaryTextColor)
        )
        viewBinding.albumCountIcon.setImageDrawable(
            getTintedDrawable(R.drawable.ic_album_white_24dp, secondaryTextColor)
        )
        viewBinding.durationText.setTextColor(secondaryTextColor)
        viewBinding.songCountText.setTextColor(secondaryTextColor)
        viewBinding.albumCountText.setTextColor(secondaryTextColor)
    }

    private suspend fun updateArtistInfo(artist: Artist) {
        viewModel.loadArtistImage(this, artist, viewBinding.image)
        viewBinding.toolbar.title = artist.name
        viewBinding.songCountText.text = songCountString(this, artist.songCount)
        viewBinding.albumCountText.text = albumCountString(this, artist.albumCount)
        val songs = withContext(Dispatchers.IO) { Songs.artist(this@ArtistDetailActivity, artist.id) }
        viewBinding.durationText.text = Durations.short(
            songs.fold(0L) { acc: Long, song: Song -> acc + song.duration }
        )
    }

    private inner class MediaStoreListener : EventHub.LifeCycleEventReceiver(this, EventHub.EVENT_MUSIC_LIBRARY_CHANGED) {
        override fun onEventReceived(context: Context, intent: Intent) {
            viewModel.load(this@ArtistDetailActivity)
        }
    }

    override val paletteColor: StateFlow<Int> get() = viewModel.paletteColor

    companion object {
        private const val EXTRA_ARTIST_ID = "extra_artist_id"

        fun launchIntent(from: Context, artistId: Long): Intent =
            Intent(from, ArtistDetailActivity::class.java).apply {
                putExtra(EXTRA_ARTIST_ID, artistId)
            }

        private fun parseIntent(intent: Intent): Long = intent.extras?.getLong(EXTRA_ARTIST_ID) ?: -1
    }


    class ArtistAlbumDisplayAdapter(activity: FragmentActivity, presenter: DisplayPresenter<Album>) :
            DisplayAdapter<Album>(activity, presenter) {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<Album> {
            val itemLayout = Layouts.itemLayoutStyle(ItemLayoutStyle.from(viewType))
            val view = LayoutInflater.from(activity).inflate(itemLayout, parent, false)
            return HorizontalAlbumViewHolder(view)
        }

        class HorizontalAlbumViewHolder(itemView: View) : DisplayViewHolder<Album>(itemView) {

            override fun bind(
                item: Album,
                position: Int,
                dataset: List<Album>,
                presenter: DisplayPresenter<Album>,
                controller: MultiSelectionController<Album>,
            ) {
                super.bind(item, position, dataset, presenter, controller)
                // setup margin
                with(itemView) {
                    val min = resources.getDimensionPixelSize(R.dimen.grid_item_margin_min)
                    val extra = resources.getDimensionPixelSize(R.dimen.grid_item_margin_extra)
                    val params = layoutParams as ViewGroup.MarginLayoutParams
                    params.marginStart = min
                    params.marginEnd = min
                    if (position == 0) { // Left
                        params.marginStart += extra
                    } else if (position == dataset.size - 1) { // Right
                        params.marginEnd += extra
                    }
                }
            }

            override fun setPaletteColors(color: Int) {
                super.setPaletteColors(color)
                (itemView as CardView).setCardBackgroundColor(color)
            }
        }
    }


    class ArtistAlbumDisplayPresenter(override val usePalette: Boolean) : AlbumBasicDisplayPresenter(SortMode(SortRef.YEAR)) {

        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.GRID_CARD_HORIZONTAL

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE

        override fun getDescription(context: Context, item: Album): CharSequence =
            buildInfoString(readableYear(item.year), songCountString(context, item.songCount))
    }


    object ArtistSongDisplayPresenter : SongBasicDisplayPresenter(SortMode(SortRef.YEAR)) {

        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST

        override val usePalette: Boolean get() = false

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE

    }
}
