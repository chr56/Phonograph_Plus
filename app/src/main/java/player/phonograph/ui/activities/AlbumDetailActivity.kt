package player.phonograph.ui.activities

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
import player.phonograph.databinding.ActivityAlbumDetailBinding
import player.phonograph.mechanism.actions.DetailToolbarMenuProviders
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.misc.IPaletteColorProvider
import player.phonograph.model.Album
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.getYearString
import player.phonograph.model.songCountString
import player.phonograph.model.totalDuration
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.fragments.pages.adapter.SongDisplayAdapter
import player.phonograph.util.NavigationUtil.goToArtist
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import util.theme.view.menu.tintOverflowButtonColor
import util.theme.view.menu.tintToolbarMenuActionIcons
import util.theme.view.toolbar.setToolbarColor
import androidx.activity.addCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Be careful when changing things in this Activity!
 */
class AlbumDetailActivity : AbsSlidingMusicPanelActivity(), IPaletteColorProvider,
                            ICreateFileStorageAccessible, IOpenFileStorageAccessible, IOpenDirStorageAccessible {

    private lateinit var viewBinding: ActivityAlbumDetailBinding
    private val model: AlbumDetailActivityViewModel by viewModel { parametersOf(parseIntent(intent)) }

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()


    private lateinit var songAdapter: SongDisplayAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        model.loadDataSet(this)

        viewBinding = ActivityAlbumDetailBinding.inflate(layoutInflater)

        autoSetStatusBarColor = false
        autoSetNavigationBarColor = false
        autoSetTaskDescriptionColor = false


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
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                model.album.collect { album ->
                    if (album.id >= 0) {
                        updateAlbumsInfo(album)
                        model.loadAlbumImage(this@AlbumDetailActivity, album, viewBinding.image)
                    }
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                model.songs.collect {
                    songAdapter.dataset = it
                }
            }
        }
        lifecycleScope.launch {
            model.paletteColor.collect {
                updateColors(it)
            }
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
        songAdapter = AlbumSongDisplayAdapter(this)
        linearLayoutManager = LinearLayoutManager(this@AlbumDetailActivity)
        with(viewBinding.recyclerView) {
            layoutManager = linearLayoutManager
            adapter = songAdapter
        }
        // Links
        viewBinding.artistText.setOnClickListener {
            val album = model.album.value
            if (album.artistName != null) {
                goToArtist(this, album.artistName, null)
            } else {
                goToArtist(this, album.artistId, null)
            }
        }
        // AppBar
        viewBinding.innerAppBar.addOnOffsetChangedListener { _, verticalOffset ->
            viewBinding.recyclerView.setPaddingTop(viewBinding.innerAppBar.totalScrollRange + verticalOffset)
        }
    }

    private fun RecyclerView.setPaddingTop(top: Int) = setPadding(paddingLeft, top, paddingRight, paddingBottom)

    private fun updateColors(color: Int) {
        viewBinding.recyclerView.setUpFastScrollRecyclerViewColor(this, color)
        viewBinding.header.setBackgroundColor(color)


        viewBinding.toolbar.setBackgroundColor(color)
        setSupportActionBar(viewBinding.toolbar) // needed to auto readjust the toolbar content color
        updateSystemUIColors(color)
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

        viewModel.updateActivityColor(color)
    }

    override val paletteColor: StateFlow<Int> get() = model.paletteColor

    private suspend fun updateAlbumsInfo(album: Album) {
        viewBinding.toolbar.title = album.title
        viewBinding.artistText.text = album.artistName
        viewBinding.songCountText.text = songCountString(this, album.songCount)
        viewBinding.durationText.text = getReadableDurationString(Songs.album(this, album.id).totalDuration())
        viewBinding.albumYearText.text = getYearString(album.year)
    }

    private fun setupMenu(menu: Menu) {
        val iconColor = primaryTextColor(viewModel.activityColor.value)
        DetailToolbarMenuProviders.AlbumToolbarMenuProvider.inflateMenu(menu, this, model.album.value, iconColor)
        tintToolbarMenuActionIcons(menu, iconColor)
        tintOverflowButtonColor(this, iconColor)
    }

    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() {
            model.loadDataSet(this@AlbumDetailActivity)
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

}
