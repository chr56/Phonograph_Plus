package player.phonograph.ui.activities

import lib.phonograph.misc.menuProvider
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
import util.theme.view.menu.tintMenuActionIcons
import util.theme.view.toolbar.setToolbarColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
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
class AlbumDetailActivity : AbsSlidingMusicPanelActivity(), IPaletteColorProvider {

    private lateinit var viewBinding: ActivityAlbumDetailBinding
    private val model: AlbumDetailActivityViewModel by viewModel { parametersOf(parseIntent(intent)) }

    private lateinit var adapter: SongDisplayAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        model.loadDataSet(this)

        viewBinding = ActivityAlbumDetailBinding.inflate(layoutInflater)

        autoSetStatusBarColor = false
        autoSetNavigationBarColor = false
        autoSetTaskDescriptionColor = false
        super.onCreate(savedInstanceState)

        // activity
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setToolbarColor(viewBinding.toolbar, primaryColor())

        // content
        setUpViews()

        // MediaStore
        lifecycle.addObserver(MediaStoreListener())

        // Observer
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                model.album.collect {
                    if (it.id >= 0) updateAlbumsInfo(it)
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                model.songs.collect {
                    adapter.dataset = it
                }
            }
        }
    }

    override fun createContentView(): View = wrapSlidingMusicPanel(viewBinding.root)

    private fun setUpViews() {
        viewBinding.innerAppBar.addOnOffsetChangedListener { _, verticalOffset ->
            viewBinding.recyclerView.setPaddingTop(viewBinding.innerAppBar.totalScrollRange + verticalOffset)
        }
        // setUpSongsAdapter
        adapter = AlbumSongDisplayAdapter(this)
        viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerView.adapter = adapter
        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                if (adapter.itemCount == 0) finish()
            }
        })
        model.isRecyclerViewPrepared = true
        // jump
        viewBinding.artistText.setOnClickListener {
            val album = model.album.value
            if (album.artistName != null) {
                goToArtist(this, album.artistName, null)
            } else {
                goToArtist(this, album.artistId, null)
            }
        }
        // paletteColor
        lifecycleScope.launch {
            model.paletteColor.collect {
                updateColors(it)
            }
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
        supportActionBar!!.title = album.title
        viewBinding.artistText.text = album.artistName
        viewBinding.songCountText.text = songCountString(this, album.songCount)
        viewBinding.durationText.text = getReadableDurationString(Songs.album(this, album.id).totalDuration())
        viewBinding.albumYearText.text = getYearString(album.year)
        model.loadAlbumImage(this, album, viewBinding.image)
    }

    private fun setupMenu(menu: Menu) {
        DetailToolbarMenuProviders.AlbumToolbarMenuProvider
            .inflateMenu(menu, this, model.album.value, primaryTextColor(viewModel.activityColor.value))
        tintMenuActionIcons(viewBinding.toolbar, menu, primaryTextColor(viewModel.activityColor.value))
    }

    @Deprecated("-") // todo: Use `OnBackPressedCallback`
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        viewBinding.recyclerView.stopScroll()
        super.onBackPressed()
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
