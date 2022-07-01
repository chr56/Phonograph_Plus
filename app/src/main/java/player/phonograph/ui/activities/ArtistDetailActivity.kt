package player.phonograph.ui.activities

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.bumptech.glide.Glide
import java.util.*
import kotlin.math.max
import kotlin.math.min
import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.createToolbarCab
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.legacy.ArtistSongAdapter
import player.phonograph.adapter.legacy.HorizontalAlbumAdapter
import player.phonograph.databinding.ActivityArtistDetailBinding
import player.phonograph.dialogs.AddToPlaylistDialog.Companion.create
import player.phonograph.dialogs.SleepTimerDialog
import player.phonograph.glide.ArtistGlideRequest
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.util.CustomArtistImageUtil.resetCustomArtistImage
import player.phonograph.glide.util.CustomArtistImageUtil.setCustomArtistImage
import player.phonograph.interfaces.PaletteColorHolder
import player.phonograph.misc.SimpleObservableScrollViewCallbacks
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote.enqueue
import player.phonograph.service.MusicPlayerRemote.openAndShuffleQueue
import player.phonograph.service.MusicPlayerRemote.playNext
import player.phonograph.settings.Setting.Companion.instance
import player.phonograph.settings.Setting.Companion.isAllowedToDownloadMetadata
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.MusicUtil.getAlbumCountString
import player.phonograph.util.MusicUtil.getReadableDurationString
import player.phonograph.util.MusicUtil.getSongCountString
import player.phonograph.util.MusicUtil.getTotalDuration
import player.phonograph.util.NavigationUtil.openEqualizer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.MaterialColorHelper
import util.mddesign.util.ToolbarColorUtil
import util.mddesign.util.Util
import util.phonograph.lastfm.rest.LastFMRestClient
import util.phonograph.lastfm.rest.model.LastFmArtist

/**
 * Be careful when changing things in this Activity!
 */
class ArtistDetailActivity : AbsSlidingMusicPanelActivity(), PaletteColorHolder {
    private lateinit var viewBinding: ActivityArtistDetailBinding

    private var songListHeader: View? = null
    private var albumRecyclerView: RecyclerView? = null

    private var headerViewHeight = 0

    override var paletteColor = 0
        private set

    private var biography: Spanned? = null
    private var biographyDialog: MaterialDialog? = null
    private var albumAdapter: HorizontalAlbumAdapter? = null
    private var songAdapter: ArtistSongAdapter? = null
    private var lastFMRestClient: LastFMRestClient? = null
    private var loader: ArtistDetailActivityLoader? = null

    private var cab: ToolbarCab? = null
    private var cabController: MultiSelectionCabController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        val artistID = intent.extras!!.getLong(EXTRA_ARTIST_ID)
        loader = ArtistDetailActivityLoader(artistID)
        load()
        viewBinding = ActivityArtistDetailBinding.inflate(layoutInflater)
        autoSetStatusBarColor = false
        autoSetNavigationBarColor = false
        autoSetTaskDescriptionColor = false
        super.onCreate(savedInstanceState)
        lastFMRestClient = LastFMRestClient(this)
        usePalette = instance().albumArtistColoredFooters
        cab = createToolbarCab(this, R.id.cab_stub, R.id.multi_selection_cab)
        cabController = MultiSelectionCabController(cab!!)
        initViews()
        setUpObservableListViewParams()
        setUpToolbar()
        setUpViews()
    }

    override fun createContentView(): View {
        return wrapSlidingMusicPanel(viewBinding.root)
    }

    private var usePalette = false
    private fun setUpObservableListViewParams() {
        headerViewHeight = resources.getDimensionPixelSize(R.dimen.detail_header_height)
    }

    private fun initViews() {
        songListHeader = LayoutInflater.from(this).inflate(R.layout.artist_detail_header, viewBinding.list, false)
        albumRecyclerView = songListHeader!!.findViewById(R.id.recycler_view)
    }

    private fun setUpViews() {
        setUpSongListView()
        setUpAlbumRecyclerView()
        loader!!.isRecyclerViewPrepared = true
        setColors(Util.resolveColor(this, R.attr.defaultFooterColor))
    }

    private fun setUpSongListView() {
        setUpSongListPadding()
        viewBinding.list.setScrollViewCallbacks(observableScrollViewCallbacks)
        viewBinding.list.addHeaderView(songListHeader)
        songAdapter = ArtistSongAdapter(this, cabController, artist.songs)
        viewBinding.list.adapter = songAdapter
        val contentView = window.decorView.findViewById<View>(android.R.id.content)
        contentView.post { observableScrollViewCallbacks.onScrollChanged(-headerViewHeight, b = false, b2 = false) }
    }

    private fun setUpSongListPadding() {
        viewBinding.list.setPadding(0, headerViewHeight, 0, 0)
    }

    private fun setUpAlbumRecyclerView() {
        albumRecyclerView!!.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        albumAdapter = HorizontalAlbumAdapter(this, artist.albums, usePalette, cabController!!)
        albumRecyclerView!!.adapter = albumAdapter
        albumAdapter!!.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                if (albumAdapter!!.itemCount == 0) finish()
            }
        })
    }

    private fun setUsePalette(usePalette: Boolean) {
        albumAdapter!!.usePalette = usePalette
        instance().albumArtistColoredFooters = usePalette
        this.usePalette = usePalette
    }

    private fun load() {
        loader!!.loadDataSet(
            this, { artist: Artist ->
            setUpArtist(artist)
        }, { songs: List<Song> ->
            songAdapter!!.dataSet = songs
        }
        ) { albums: List<Album> ->
            albumAdapter!!.dataSet = albums
        }
    }

    private fun loadBiography(lang: String? = Locale.getDefault().language) {
        biography = null
        lastFMRestClient!!.apiService
            .getArtistInfo(artist.name, lang, null)
            .enqueue(object : Callback<LastFmArtist?> {
                override fun onResponse(
                    call: Call<LastFmArtist?>,
                    response: Response<LastFmArtist?>
                ) {
                    val lastFmArtist = response.body()
                    if (lastFmArtist != null && lastFmArtist.artist != null) {
                        val bioContent = lastFmArtist.artist.bio.content
                        if (bioContent != null && bioContent.trim { it <= ' ' }.isNotEmpty()) {
                            biography = Html.fromHtml(bioContent, Html.FROM_HTML_MODE_LEGACY)
                        }
                    }

                    // If the "lang" parameter is set and no biography is given, retry with default language
                    if (biography == null && lang != null) {
                        loadBiography(null)
                        return
                    }
                    if (!isAllowedToDownloadMetadata(this@ArtistDetailActivity)) {
                        if (biography != null) {
                            biographyDialog!!.message(null, biography, null)
                        } else {
                            biographyDialog!!.dismiss()
                            Toast.makeText(
                                this@ArtistDetailActivity,
                                resources.getString(R.string.biography_unavailable),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onFailure(call: Call<LastFmArtist?>, t: Throwable) {
                    t.printStackTrace()
                    biography = null
                }
            })
    }

    private fun loadArtistImage() {
        ArtistGlideRequest.Builder.from(Glide.with(this), artist)
            .generatePalette(this).build()
            .dontAnimate()
            .into(object : PhonographColoredTarget(viewBinding.image) {
                override fun onColorReady(color: Int) {
                    setColors(color)
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SELECT_IMAGE -> if (resultCode == RESULT_OK) {
                setCustomArtistImage(artist, data!!.data!!)
            }
            else -> if (resultCode == RESULT_OK) {
                load()
            }
        }
    }

    private fun setColors(color: Int) {
        paletteColor = color
        viewBinding.header.setBackgroundColor(color)
        setNavigationbarColor(color)
        setTaskDescriptionColor(color)
        viewBinding.toolbar.setBackgroundColor(color)
        setSupportActionBar(viewBinding.toolbar) // needed to auto readjust the toolbar content color
        viewBinding.toolbar.setTitleTextColor(ToolbarColorUtil.toolbarTitleColor(this, color))
        setStatusbarColor(color)
        val secondaryTextColor = MaterialColorHelper.getSecondaryTextColor(this, ColorUtil.isColorLight(color))
        val f = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(secondaryTextColor, BlendModeCompat.SRC_IN)
        viewBinding.durationIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_timer_white_24dp))
        viewBinding.durationIcon.colorFilter = f
        viewBinding.songCountIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_music_note_white_24dp))
        viewBinding.songCountIcon.colorFilter = f
        viewBinding.albumCountIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_album_white_24dp))
        viewBinding.albumCountIcon.colorFilter = f
        viewBinding.durationIcon.setColorFilter(secondaryTextColor, PorterDuff.Mode.SRC_IN)
        viewBinding.songCountIcon.setColorFilter(secondaryTextColor, PorterDuff.Mode.SRC_IN)
        viewBinding.albumCountIcon.setColorFilter(secondaryTextColor, PorterDuff.Mode.SRC_IN)
        viewBinding.durationText.setTextColor(secondaryTextColor)
        viewBinding.songCountText.setTextColor(secondaryTextColor)
        viewBinding.albumCountText.setTextColor(secondaryTextColor)
        cabController!!.cabColor = color
        activityColor = color
    }

    private fun setUpToolbar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_artist_detail, menu)
        menu.findItem(R.id.action_colored_footers).isChecked = usePalette
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val songs = songAdapter!!.dataSet
        when (id) {
            R.id.action_sleep_timer -> {
                SleepTimerDialog().show(supportFragmentManager, "SET_SLEEP_TIMER")
                return true
            }
            R.id.action_equalizer -> {
                openEqualizer(this)
                return true
            }
            R.id.action_shuffle_artist -> {
                openAndShuffleQueue(songs, true)
                return true
            }
            R.id.action_play_next -> {
                playNext(songs)
                return true
            }
            R.id.action_add_to_current_playing -> {
                enqueue(songs)
                return true
            }
            R.id.action_add_to_playlist -> {
                create(songs).show(supportFragmentManager, "ADD_PLAYLIST")
                return true
            }
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
            R.id.action_biography -> {
                if (biographyDialog == null) {
                    biographyDialog = MaterialDialog(this)
                        .title(null, artist.name)
                        .positiveButton(android.R.string.ok, null, null)
                    // set button color
                    biographyDialog!!.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(this))
                }
                if (isAllowedToDownloadMetadata(this@ArtistDetailActivity)) { // wiki should've been already downloaded
                    if (biography != null) {
                        biographyDialog!!.message(null, biography, null)
                        biographyDialog!!.show()
                    } else {
                        Toast.makeText(this@ArtistDetailActivity, resources.getString(R.string.biography_unavailable), Toast.LENGTH_SHORT)
                            .show()
                    }
                } else { // force download
                    biographyDialog!!.show()
                    loadBiography()
                }
                return true
            }
            R.id.action_set_artist_image -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_from_local_storage)), REQUEST_CODE_SELECT_IMAGE)
                return true
            }
            R.id.action_reset_artist_image -> {
                Toast.makeText(this@ArtistDetailActivity, resources.getString(R.string.updating), Toast.LENGTH_SHORT).show()
                resetCustomArtistImage(artist)
                return true
            }
            R.id.action_colored_footers -> {
                item.isChecked = !item.isChecked
                setUsePalette(item.isChecked)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (!cabController!!.dismiss()) {
            albumRecyclerView!!.stopScroll()
            super.onBackPressed()
        }
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        load()
    }

    override fun setStatusbarColor(color: Int) {
        super.setStatusbarColor(color)
        setLightStatusbar(false)
    }

    private fun setUpArtist(artist: Artist) {
        loadArtistImage()
        if (isAllowedToDownloadMetadata(this)) {
            loadBiography()
        }
        supportActionBar!!.title = artist.name
        viewBinding.songCountText.text = getSongCountString(this, artist.songCount)
        viewBinding.albumCountText.text = getAlbumCountString(this, artist.albumCount)
        viewBinding.durationText.text = getReadableDurationString(getTotalDuration(this, artist.songs))

        // songAdapter.swapDataSet(artist.getSongs());
        // albumAdapter.swapDataSet(artist.albums);
    }

    private val artist: Artist
        get() = if (loader!!._artist != null) loader!!.artist else Artist()

    private val observableScrollViewCallbacks: SimpleObservableScrollViewCallbacks = object : SimpleObservableScrollViewCallbacks() {
        override fun onScrollChanged(i: Int, b: Boolean, b2: Boolean) {
            val scrollY = i + headerViewHeight

            // Change alpha of overlay
            val headerAlpha = max(0f, min(1f, 2f * scrollY / headerViewHeight))
            viewBinding.headerOverlay.setBackgroundColor(ColorUtil.withAlpha(paletteColor, headerAlpha))

            // Translate name text
            viewBinding.header.translationY = max(-scrollY, -headerViewHeight).toFloat()
            viewBinding.headerOverlay.translationY = max(-scrollY, -headerViewHeight).toFloat()
            viewBinding.image.translationY = max(-scrollY, -headerViewHeight).toFloat()
        }
    }

    companion object {
        private const val REQUEST_CODE_SELECT_IMAGE = 1000
        const val EXTRA_ARTIST_ID = "extra_artist_id"
    }
}
