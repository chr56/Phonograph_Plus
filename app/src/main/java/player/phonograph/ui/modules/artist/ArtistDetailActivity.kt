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
import player.phonograph.mechanism.actions.DetailToolbarMenuProviders
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
import player.phonograph.util.component.GetContentDelegate
import player.phonograph.util.component.IGetContentRequester
import player.phonograph.util.observe
import player.phonograph.util.text.albumCountString
import player.phonograph.util.text.buildInfoString
import player.phonograph.util.text.readableDuration
import player.phonograph.util.text.readableYear
import player.phonograph.util.text.songCountString
import player.phonograph.util.text.totalDuration
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.theme.updateSystemBarsColor
import player.phonograph.util.ui.BottomViewWindowInsetsController
import player.phonograph.util.ui.applyControllableWindowInsetsAsBottomView
import player.phonograph.util.ui.menuProvider
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import util.theme.color.toolbarTitleColor
import util.theme.view.menu.tintOverflowButtonColor
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
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
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

    private lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController

    private lateinit var albumAdapter: ArtistAlbumDisplayAdapter
    private lateinit var songAdapter: DisplayAdapter<Song>

    override val getContentDelegate: GetContentDelegate = GetContentDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.load(this)

        registerActivityResultLauncherDelegate(
            createFileStorageAccessDelegate,
            openFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
            getContentDelegate,
        )

        viewBinding = ActivityArtistDetailBinding.inflate(layoutInflater)

        super.onCreate(savedInstanceState)

        setUpToolbar()
        setUpViews()
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

    override fun createContentView(): View = wrapSlidingMusicPanel(viewBinding.root)

    private fun setUpViews() {
        viewBinding.innerAppBar.addOnOffsetChangedListener { _, verticalOffset ->
            viewBinding.mainContent.setPaddingTop(verticalOffset)
        }

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
        setColors(themeFooterColor(this))
        // WindowInsets
        bottomViewWindowInsetsController = viewBinding.songsRecycleView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isPanelHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }
    }

    private fun observeData() {
        observe(viewModel.artist) { artist -> updateArtistInfo(artist ?: Artist()) }
        observe(viewModel.albums) { albums -> albumAdapter.dataset = albums ?: emptyList() }
        observe(viewModel.songs) { songs -> songAdapter.dataset = songs ?: emptyList() }
        observe(viewModel.paletteColor) { color -> setColors(color) }
        observe(viewModel.usePaletteColor) { it ->
            albumAdapter.presenter = ArtistAlbumDisplayPresenter(it)
            val dataset = albumAdapter.dataset
            synchronized(albumAdapter) {
                albumAdapter.dataset = emptyList()
                albumAdapter.dataset = dataset
            }
        }
    }

    private fun setColors(color: Int) {
        viewBinding.header.setBackgroundColor(color)

        setSupportActionBar(viewBinding.toolbar) // needed to auto readjust the toolbar content color
        setToolbarColor(viewBinding.toolbar, color)
        viewBinding.toolbar.setTitleTextColor(toolbarTitleColor(this, color))

        updateSystemBarsColor(color, Color.TRANSPARENT)
        val secondaryTextColor = secondaryTextColor(color)
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
        panelViewModel.updateActivityColor(color)
    }

    override val paletteColor: StateFlow<Int> get() = viewModel.paletteColor

    private fun setUpToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setToolbarColor(viewBinding.toolbar, primaryColor())
    }

    private fun setupMenu(menu: Menu) {
        val iconColor = primaryTextColor(panelViewModel.activityColor.value)
        DetailToolbarMenuProviders.ArtistToolbarMenuProvider.inflateMenu(
            menu, this, viewModel.artist.value ?: Artist(), iconColor
        )
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
    }

    private inner class MediaStoreListener : EventHub.LifeCycleEventReceiver(this, EventHub.EVENT_MEDIASTORE_CHANGED) {
        override fun onEventReceived(context: Context, intent: Intent) {
            viewModel.load(this@ArtistDetailActivity)
        }
    }

    private suspend fun updateArtistInfo(artist: Artist) {
        viewModel.loadArtistImage(this, artist, viewBinding.image)
        supportActionBar!!.title = artist.name
        viewBinding.songCountText.text = songCountString(this, artist.songCount)
        viewBinding.albumCountText.text = albumCountString(this, artist.albumCount)
        val songs = withContext(Dispatchers.IO) {
            Songs.artist(this@ArtistDetailActivity, artist.id)
        }
        viewBinding.durationText.text = readableDuration(totalDuration(songs))
    }


    private fun View.setPaddingTop(top: Int) = setPadding(paddingLeft, top, paddingRight, paddingBottom)

    companion object {
        private const val EXTRA_ARTIST_ID = "extra_artist_id"

        fun launchIntent(from: Context, artistId: Long): Intent =
            Intent(from, ArtistDetailActivity::class.java).apply {
                putExtra(EXTRA_ARTIST_ID, artistId)
            }

        private fun parseIntent(intent: Intent): Long = intent.extras?.getLong(EXTRA_ARTIST_ID) ?: -1
    }


    private class ArtistAlbumDisplayAdapter(activity: FragmentActivity, presenter: DisplayPresenter<Album>) :
            DisplayAdapter<Album>(activity, presenter) {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder<Album> {
            val view = LayoutInflater.from(activity).inflate(ItemLayoutStyle.from(viewType).layout(), parent, false)
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
                with(itemView) {
                    (layoutParams as MarginLayoutParams).updateMargin(
                        resources,
                        position == 0,
                        position == dataset.size - 1
                    )
                }
            }

            private fun MarginLayoutParams.updateMargin(resources: Resources, left: Boolean, right: Boolean) {
                val listMargin = resources.getDimensionPixelSize(R.dimen.default_item_margin)
                marginStart = 8
                marginEnd = 8
                if (left) {
                    marginStart += listMargin
                } else if (right) {
                    marginEnd += listMargin
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
