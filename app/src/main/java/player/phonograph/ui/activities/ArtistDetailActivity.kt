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
import player.phonograph.R
import player.phonograph.actions.menu.artistDetailToolbar
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.adapter.legacy.HorizontalAlbumAdapter
import player.phonograph.coil.CustomArtistImageStore
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.databinding.ActivityArtistDetailBinding
import player.phonograph.misc.PaletteColorHolder
import player.phonograph.misc.menuProvider
import player.phonograph.model.Artist
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Be careful when changing things in this Activity!
 */
class ArtistDetailActivity : AbsSlidingMusicPanelActivity(), PaletteColorHolder {

    private lateinit var viewBinding: ActivityArtistDetailBinding
    private lateinit var model: ArtistDetailActivityViewModel

    private lateinit var albumAdapter: HorizontalAlbumAdapter
    private lateinit var songAdapter: SongDisplayAdapter

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
        model.load(this)

        autoSetStatusBarColor = false
        autoSetNavigationBarColor = false
        autoSetTaskDescriptionColor = false

        super.onCreate(savedInstanceState)

        setUpToolbar()
        setUpViews()
        observeData()
    }

    override fun createContentView(): View = wrapSlidingMusicPanel(viewBinding.root)

    private fun setUpViews() {
        viewBinding.innerAppBar.addOnOffsetChangedListener { _, verticalOffset ->
            viewBinding.mainContent.setPaddingTop(verticalOffset)
        }

        songAdapter =
            SongDisplayAdapter(this, cabController, emptyList(), R.layout.item_list, null)
        with(viewBinding.songsRecycleView) {
            adapter = songAdapter
            layoutManager =
                LinearLayoutManager(this@ArtistDetailActivity, VERTICAL, false)
        }

        albumAdapter =
            HorizontalAlbumAdapter(this, emptyList(), usePalette, cabController)
        with(viewBinding.albumRecycleView) {
            adapter = albumAdapter
            layoutManager =
                LinearLayoutManager(this@ArtistDetailActivity, HORIZONTAL, false)
            albumAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
                override fun onChanged() {
                    super.onChanged()
                    if (albumAdapter.itemCount == 0) finish()
                }
            })
        }

        setColors(resolveColor(this, R.attr.defaultFooterColor))
    }

    private fun observeData() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.artist.collect {
                    setUpArtist(it ?: Artist())
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.albums.collect {
                    albumAdapter.dataSet = it ?: emptyList()
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.songs.collect {
                    songAdapter.dataset = it ?: emptyList()
                }
            }
        }
    }


    private var biography: Spanned? = null
    private var biographyDialog: MaterialDialog? = null
    private var lastFmUrl: String? = null

    private fun loadBiography(artist: Artist, lang: String? = Locale.getDefault().language) {
        biography = null
        lastFMRestClient.apiService
            .getArtistInfo(artist.name, lang, null)
            .enqueue(object : Callback<LastFmArtist?> {
                override fun onResponse(
                    call: Call<LastFmArtist?>,
                    response: Response<LastFmArtist?>,
                ) {

                    response.body()?.let { lastFmArtist ->
                        lastFmUrl = lastFmArtist.artist.url
                        val bioContent = lastFmArtist.artist.bio?.content
                        if (bioContent != null && bioContent.trim { it <= ' ' }.isNotEmpty()) {
                            biography = Html.fromHtml(bioContent, Html.FROM_HTML_MODE_LEGACY)
                        }
                    }

                    // If the "lang" parameter is set and no biography is given, retry with default language
                    if (biography == null && lang != null) {
                        loadBiography(artist, null)
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

    private fun loadArtistImage(artist: Artist) {
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
                val artist = model.artist.value!!
                CustomArtistImageStore.instance(this)
                    .setCustomArtistImage(this, artist.id, artist.name, data!!.data!!)
            }
            else                      -> if (resultCode == RESULT_OK) {
                model.load(this)
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
        artistDetailToolbar(
            menu = menu,
            context = this,
            artist = model.artist.value ?: Artist(),
            iconColor = primaryTextColor(activityColor),
            loadBiographyCallback = this::biographyCallback
        )
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
            loadBiography(artist)
        }
        return true
    }


    override fun onBackPressed() {
        if (!cabController.dismiss()) {
            viewBinding.albumRecycleView.stopScroll()
            viewBinding.songsRecycleView.stopScroll()
            super.onBackPressed()
        }
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        model.load(this)
    }

    override fun setStatusbarColor(color: Int) {
        super.setStatusbarColor(color)
        requireLightStatusbar(false)
    }

    private fun setUpArtist(artist: Artist) {
        loadArtistImage(artist)
        if (Setting.instance.isAllowedToDownloadMetadata(this)) {
            loadBiography(artist)
        }
        supportActionBar!!.title = artist.name
        viewBinding.songCountText.text = songCountString(this, artist.songCount)
        viewBinding.albumCountText.text = albumCountString(this, artist.albumCount)
        viewBinding.durationText.text = getReadableDurationString(artist.songs.totalDuration())
    }


    private fun View.setPaddingTop(top: Int) =
        setPadding(paddingLeft, top, paddingRight, paddingBottom)

    companion object {
        const val REQUEST_CODE_SELECT_IMAGE = 1000
        const val EXTRA_ARTIST_ID = "extra_artist_id"
    }
}
