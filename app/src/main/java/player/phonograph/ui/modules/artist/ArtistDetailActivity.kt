package player.phonograph.ui.modules.artist

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
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
import player.phonograph.databinding.ActivityArtistDetailBinding
import player.phonograph.mechanism.actions.DetailToolbarMenuProviders
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.IPaletteColorProvider
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.Song
import player.phonograph.model.albumCountString
import player.phonograph.model.buildInfoString
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.getYearString
import player.phonograph.model.songCountString
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.totalDuration
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.adapter.AlbumBasicDisplayPresenter
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.ui.adapter.SongBasicDisplayPresenter
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.util.component.GetContentDelegate
import player.phonograph.util.component.IGetContentRequester
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.theme.updateSystemBarsColor
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import util.theme.color.toolbarTitleColor
import util.theme.view.menu.tintOverflowButtonColor
import util.theme.view.menu.tintToolbarMenuActionIcons
import util.theme.view.toolbar.setToolbarColor
import androidx.activity.addCallback
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ArtistDetailActivity : AbsSlidingMusicPanelActivity(), IPaletteColorProvider, IGetContentRequester,
                             ICreateFileStorageAccessible, IOpenFileStorageAccessible, IOpenDirStorageAccessible {

    private lateinit var viewBinding: ActivityArtistDetailBinding
    private val viewModel: ArtistDetailActivityViewModel by viewModel { parametersOf(parseIntent(intent)) }

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()


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
    }

    private fun observeData() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.usePaletteColor.collect {
                    albumAdapter.presenter = ArtistAlbumDisplayPresenter(it)
                    val dataset = albumAdapter.dataset
                    synchronized(albumAdapter) {
                        albumAdapter.dataset = emptyList()
                        albumAdapter.dataset = dataset
                    }
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.artist.collect {
                    updateArtistInfo(it ?: Artist())
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.albums.collect {
                    albumAdapter.dataset = it ?: emptyList()
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.songs.collect {
                    songAdapter.dataset = it ?: emptyList()
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.paletteColor.collect { color ->
                    setColors(color)
                }
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
            menuItem(title = getString(R.string.colored_footers)) {
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

    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() {
            viewModel.load(this@ArtistDetailActivity)
        }
    }

    private suspend fun updateArtistInfo(artist: Artist) {
        viewModel.loadArtistImage(this, artist, viewBinding.image)
        supportActionBar!!.title = artist.name
        viewBinding.songCountText.text = songCountString(this, artist.songCount)
        viewBinding.albumCountText.text = albumCountString(this, artist.albumCount)
        viewBinding.durationText.text = getReadableDurationString(Songs.artist(this, artist.id).totalDuration())
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
            buildInfoString(getYearString(item.year), songCountString(context, item.songCount))
    }


    object ArtistSongDisplayPresenter : SongBasicDisplayPresenter(SortMode(SortRef.YEAR)) {

        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST

        override val usePalette: Boolean get() = false

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE

    }
}
