package player.phonograph.ui.activities

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.createToolbarCab
import mt.pref.ThemeColor.primaryColor
import mt.tint.requireLightStatusbar
import mt.tint.setActivityToolbarColor
import mt.tint.setActivityToolbarColorAuto
import mt.tint.setNavigationBarColor
import mt.tint.viewtint.tintMenuActionIcons
import mt.util.color.primaryTextColor
import mt.util.color.resolveColor
import mt.util.color.secondaryTextColor
import mt.util.color.toolbarTitleColor
import mt.util.color.withAlpha
import player.phonograph.R
import player.phonograph.actions.menu.artistDetailToolbar
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.legacy.ArtistSongAdapter
import player.phonograph.adapter.legacy.HorizontalAlbumAdapter
import player.phonograph.coil.CustomArtistImageStore
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.databinding.ActivityArtistDetailBinding
import player.phonograph.interfaces.PaletteColorHolder
import player.phonograph.misc.SimpleObservableScrollViewCallbacks
import player.phonograph.misc.menuProvider
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.model.albumCountString
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.songCountString
import player.phonograph.model.totalDuration
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.ImageUtil.getTintedDrawable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import util.phonograph.lastfm.rest.LastFMRestClient
import util.phonograph.lastfm.rest.model.LastFmArtist
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlin.math.max
import kotlin.math.min
import java.util.Locale

/**
 * Be careful when changing things in this Activity!
 */
class ArtistDetailActivity : AbsSlidingMusicPanelActivity(), PaletteColorHolder {

    private lateinit var viewBinding: ActivityArtistDetailBinding
    private lateinit var model: ArtistDetailActivityViewModel

    private lateinit var songListHeader: View
    private lateinit var albumRecyclerView: RecyclerView

    private var headerViewHeight = 0

    private var biography: Spanned? = null
    private var biographyDialog: MaterialDialog? = null
    private var lastFmUrl: String? = null

    private lateinit var albumAdapter: HorizontalAlbumAdapter
    private lateinit var songAdapter: ArtistSongAdapter

    private val lastFMRestClient: LastFMRestClient by lazy { LastFMRestClient(this) }

    override var paletteColor = 0
        private set
    private var usePalette = Setting.instance.albumArtistColoredFooters
        set(value) {
            field = value
            Setting.instance.albumArtistColoredFooters = usePalette
            albumAdapter.usePalette = usePalette
        }

    private lateinit var cab: ToolbarCab
    private lateinit var cabController: MultiSelectionCabController

    override fun onCreate(savedInstanceState: Bundle?) {
        model = ArtistDetailActivityViewModel(intent.extras!!.getLong(EXTRA_ARTIST_ID))
        viewBinding = ActivityArtistDetailBinding.inflate(layoutInflater)
        load()

        autoSetStatusBarColor = false
        autoSetNavigationBarColor = false
        autoSetTaskDescriptionColor = false

        super.onCreate(savedInstanceState)

        songListHeader = LayoutInflater.from(this).inflate(R.layout.artist_detail_header, viewBinding.list, false)
        albumRecyclerView = songListHeader.findViewById(R.id.recycler_view)

        // ObservableListViewParams
        headerViewHeight = resources.getDimensionPixelSize(R.dimen.detail_header_height)

        setUpToolbar()
        setUpViews()
    }

    override fun createContentView(): View = wrapSlidingMusicPanel(viewBinding.root)

    private fun setUpViews() {
        setUpSongListView()
        setUpAlbumRecyclerView()
        model.isRecyclerViewPrepared = true
        setColors(resolveColor(this, R.attr.defaultFooterColor))
    }

    private fun setUpSongListView() {
        songAdapter = ArtistSongAdapter(this@ArtistDetailActivity, cabController, artist.songs)
        with(viewBinding.list) {
            setPadding(0, headerViewHeight, 0, 0)
            setScrollViewCallbacks(observableScrollViewCallbacks)
            addHeaderView(songListHeader)
            adapter = songAdapter
        }
        window.decorView.findViewById<View>(android.R.id.content).post {
            observableScrollViewCallbacks.onScrollChanged(-headerViewHeight, b = false, b2 = false)
        }
    }

    private fun setUpAlbumRecyclerView() {
        albumRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        albumAdapter = HorizontalAlbumAdapter(this, artist.albums, usePalette, cabController)
        albumRecyclerView.adapter = albumAdapter
        albumAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                if (albumAdapter.itemCount == 0) finish()
            }
        })
    }

    private fun load() {
        model.loadDataSet(
            this,
            { artist: Artist ->
                setUpArtist(artist)
            },
            { songs: List<Song> ->
                songAdapter.dataSet = songs
            },
            { albums: List<Album> ->
                albumAdapter.dataSet = albums
            }
        )
    }

    private fun loadBiography(lang: String? = Locale.getDefault().language) {
        biography = null
        lastFMRestClient.apiService
            .getArtistInfo(artist.name, lang, null)
            .enqueue(object : Callback<LastFmArtist?> {
                override fun onResponse(
                    call: Call<LastFmArtist?>,
                    response: Response<LastFmArtist?>,
                ) {

                    response.body()?.let { lastFmArtist ->
                        lastFmUrl = lastFmArtist.artist?.url
                        val bioContent = lastFmArtist.artist?.bio?.content
                        if (bioContent != null && bioContent.trim { it <= ' ' }.isNotEmpty()) {
                            biography = Html.fromHtml(bioContent, Html.FROM_HTML_MODE_LEGACY)
                        }
                    }

                    // If the "lang" parameter is set and no biography is given, retry with default language
                    if (biography == null && lang != null) {
                        loadBiography(null)
                        return
                    }
                    if (!Setting.instance.isAllowedToDownloadMetadata(this@ArtistDetailActivity)) {
                        with(biographyDialog!!) {
                            if (biography != null) {
                                message(text = biography)
                            } else {
                                message(R.string.biography_unavailable)
                            }
                            negativeButton(text = "Last.FM") {
                                startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(lastFmUrl)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<LastFmArtist?>, t: Throwable) {
                    ErrorNotification.postErrorNotification(t, "Load ${artist.name} Fail")
                    biography = null
                }
            })
    }

    private fun loadArtistImage() {
        val defaultColor = primaryColor(this)
        loadImage(this)
            .from(artist)
            .into(
                PaletteTargetBuilder(defaultColor)
                    .onResourceReady { result, color ->
                        viewBinding.image.setImageDrawable(result)
                        setColors(color)
                    }
                    .onFail {
                        viewBinding.image.setImageResource(R.drawable.default_album_art)
                        setColors(defaultColor)
                    }
                    .build()
            )
            .enqueue()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SELECT_IMAGE -> if (resultCode == RESULT_OK) {
                CustomArtistImageStore.instance(this)
                    .setCustomArtistImage(this, artist.id, artist.name, data!!.data!!)
            }
            else -> if (resultCode == RESULT_OK) {
                load()
            }
        }
    }

    private fun setColors(color: Int) {
        paletteColor = color
        viewBinding.header.setBackgroundColor(color)
        setNavigationBarColor(color)
        setTaskDescriptionColor(color)

        setSupportActionBar(viewBinding.toolbar) // needed to auto readjust the toolbar content color
        setActivityToolbarColor(viewBinding.toolbar, color)
        viewBinding.toolbar.setTitleTextColor(toolbarTitleColor(this, color))

        setStatusbarColor(color)
        val secondaryTextColor = secondaryTextColor(color)
        viewBinding.durationIcon.setImageDrawable(getTintedDrawable(R.drawable.ic_timer_white_24dp, secondaryTextColor))
        viewBinding.songCountIcon.setImageDrawable(getTintedDrawable(R.drawable.ic_music_note_white_24dp, secondaryTextColor))
        viewBinding.albumCountIcon.setImageDrawable(getTintedDrawable(R.drawable.ic_album_white_24dp, secondaryTextColor))
        viewBinding.durationText.setTextColor(secondaryTextColor)
        viewBinding.songCountText.setTextColor(secondaryTextColor)
        viewBinding.albumCountText.setTextColor(secondaryTextColor)
        cabController.cabColor = color
        activityColor = color
    }

    private fun setUpToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setActivityToolbarColorAuto(viewBinding.toolbar)
        // MultiSelectionCab
        cab = createToolbarCab(this, R.id.cab_stub, R.id.multi_selection_cab)
        cabController = MultiSelectionCabController(cab)
    }

    private fun setupMenu(menu: Menu) {
        artistDetailToolbar(menu, this, artist, primaryTextColor(activityColor), this::biographyCallback)
        attach(menu) {
            menuItem(title = getString(R.string.colored_footers)) {
                checkable = true
                checked = usePalette
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    it.isChecked = !it.isChecked
                    usePalette = it.isChecked
                    true
                }
            }
        }
        tintMenuActionIcons(viewBinding.toolbar, menu, primaryTextColor(activityColor))
    }

    private fun biographyCallback(artist: Artist): Boolean {
        if (biographyDialog == null) {
            biographyDialog = MaterialDialog(this)
                .title(null, artist.name)
                .positiveButton(android.R.string.ok, null, null)
                .apply {
                    getActionButton(WhichButton.POSITIVE).updateTextColor(accentColor)
                    getActionButton(WhichButton.NEGATIVE).updateTextColor(accentColor)
                }
        }
        if (Setting.instance.isAllowedToDownloadMetadata(this@ArtistDetailActivity)) { // wiki should've been already downloaded
            biographyDialog!!.show {
                if (biography != null) {
                    message(text = biography)
                } else {
                    message(R.string.biography_unavailable)
                }
                negativeButton(text = "Last.FM") {
                    startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(lastFmUrl)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }
            }
        } else { // force download
            biographyDialog!!.show()
            loadBiography()
        }
        return true
    }


    override fun onBackPressed() {
        if (!cabController.dismiss()) {
            albumRecyclerView.stopScroll()
            super.onBackPressed()
        }
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        load()
    }

    override fun setStatusbarColor(color: Int) {
        super.setStatusbarColor(color)
        requireLightStatusbar(false)
    }

    private fun setUpArtist(artist: Artist) {
        loadArtistImage()
        if (Setting.instance.isAllowedToDownloadMetadata(this)) {
            loadBiography()
        }
        supportActionBar!!.title = artist.name
        viewBinding.songCountText.text = songCountString(this, artist.songCount)
        viewBinding.albumCountText.text = albumCountString(this, artist.albumCount)
        viewBinding.durationText.text = getReadableDurationString(artist.songs.totalDuration())

        // songAdapter.swapDataSet(artist.getSongs());
        // albumAdapter.swapDataSet(artist.albums);
    }

    private val artist: Artist get() = model.artist

    private val observableScrollViewCallbacks: SimpleObservableScrollViewCallbacks = object : SimpleObservableScrollViewCallbacks() {
        override fun onScrollChanged(i: Int, b: Boolean, b2: Boolean) {
            val scrollY = i + headerViewHeight

            // Change alpha of overlay
            val headerAlpha = max(0f, min(1f, 2f * scrollY / headerViewHeight))
            viewBinding.headerOverlay.setBackgroundColor(withAlpha(paletteColor, headerAlpha))

            // Translate name text
            viewBinding.header.translationY = max(-scrollY, -headerViewHeight).toFloat()
            viewBinding.headerOverlay.translationY = max(-scrollY, -headerViewHeight).toFloat()
            viewBinding.image.translationY = max(-scrollY, -headerViewHeight).toFloat()
        }
    }

    companion object {
        const val REQUEST_CODE_SELECT_IMAGE = 1000
        const val EXTRA_ARTIST_ID = "extra_artist_id"
    }
}
