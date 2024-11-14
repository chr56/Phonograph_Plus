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
import player.phonograph.misc.IPaletteColorProvider
import player.phonograph.model.Artist
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.albumCountString
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.songCountString
import player.phonograph.model.totalDuration
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.adapter.ConstDisplayConfig
import player.phonograph.ui.modules.main.pages.adapter.SongDisplayAdapter
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.util.GetContentDelegate
import player.phonograph.util.IGetContentRequester
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.theme.themeFooterColor
import player.phonograph.util.theme.updateAllSystemUIColors
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import util.theme.color.toolbarTitleColor
import util.theme.view.menu.tintOverflowButtonColor
import util.theme.view.menu.tintToolbarMenuActionIcons
import util.theme.view.toolbar.setToolbarColor
import androidx.activity.addCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ArtistDetailActivity : AbsSlidingMusicPanelActivity(), IPaletteColorProvider, IGetContentRequester,
                             ICreateFileStorageAccessible, IOpenFileStorageAccessible, IOpenDirStorageAccessible {

    private lateinit var viewBinding: ActivityArtistDetailBinding
    private val viewModel: ArtistDetailActivityViewModel by viewModel { parametersOf(parseIntent(intent)) }

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()


    private lateinit var albumAdapter: HorizontalAlbumDisplayAdapter
    private lateinit var songAdapter: SongDisplayAdapter

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

        songAdapter = SongDisplayAdapter(this, ConstDisplayConfig(ItemLayoutStyle.LIST, false))
        with(viewBinding.songsRecycleView) {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(this@ArtistDetailActivity, VERTICAL, false)
        }

        albumAdapter = HorizontalAlbumDisplayAdapter(this)
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
                    albumAdapter.config = ConstDisplayConfig(ItemLayoutStyle.GRID_CARD_HORIZONTAL, it)
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

        updateAllSystemUIColors(color)
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
}
