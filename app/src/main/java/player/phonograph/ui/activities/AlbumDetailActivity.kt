package player.phonograph.ui.activities

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.google.android.material.appbar.AppBarLayout
import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.createToolbarCab
import mt.pref.ThemeColor.primaryColor
import mt.tint.requireLightStatusbar
import mt.tint.setActivityToolbarColor
import mt.tint.setActivityToolbarColorAuto
import mt.tint.setNavigationBarColor
import mt.tint.viewtint.tintMenuActionIcons
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.actions.menu.albumDetailToolbar
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.display.AlbumSongDisplayAdapter
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteTargetBuilder
import player.phonograph.databinding.ActivityAlbumDetailBinding
import player.phonograph.misc.menuProvider
import player.phonograph.model.Album
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.getYearString
import player.phonograph.model.songCountString
import player.phonograph.model.totalDuration
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.NavigationUtil.goToArtist
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spanned
import android.view.Menu
import android.view.MenuItem
import android.view.View

/**
 * Be careful when changing things in this Activity!
 */
class AlbumDetailActivity : AbsSlidingMusicPanelActivity() {

    companion object {
        const val TAG_EDITOR_REQUEST = 2001
        const val EXTRA_ALBUM_ID = "extra_album_id"
    }

    private lateinit var viewBinding: ActivityAlbumDetailBinding
    private val model: AlbumDetailActivityViewModel by viewModels()

    private lateinit var adapter: SongDisplayAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        val albumID = intent.extras!!.getLong(EXTRA_ALBUM_ID)
        model.albumId = albumID
        load()

        viewBinding = ActivityAlbumDetailBinding.inflate(layoutInflater)

        autoSetStatusBarColor = false
        autoSetNavigationBarColor = false
        autoSetTaskDescriptionColor = false
        super.onCreate(savedInstanceState)

        // multiselect cab
        cab = createToolbarCab(this, R.id.cab_stub, R.id.multi_selection_cab)
        cabController = MultiSelectionCabController(cab)

        // activity
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setActivityToolbarColorAuto(viewBinding.toolbar)

        // content
        setUpViews()
    }

    lateinit var cab: ToolbarCab
    lateinit var cabController: MultiSelectionCabController

    override fun createContentView(): View = wrapSlidingMusicPanel(viewBinding.root)

    private fun setUpViews() {
        viewBinding.innerAppBar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                viewBinding.recyclerView.setPaddingTop(viewBinding.innerAppBar.totalScrollRange + verticalOffset)
            }
        )
        // setUpSongsAdapter
        adapter = AlbumSongDisplayAdapter(this, cabController, album.songs, R.layout.item_list) {
            useImageText = true
            usePalette = false
        }
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
            goToArtist(this, album.artistId)
        }
        // paletteColor
        model.paletteColor.observe(this) {
            updateColors(it)
        }
    }

    private fun RecyclerView.setPaddingTop(top: Int) = setPadding(paddingLeft, top, paddingRight, paddingBottom)

    private fun updateColors(color: Int) {
        viewBinding.recyclerView.setUpFastScrollRecyclerViewColor(this, color)
        viewBinding.header.setBackgroundColor(color)
        setNavigationBarColor(color)
        setTaskDescriptionColor(color)


        viewBinding.toolbar.setBackgroundColor(color)
        setSupportActionBar(viewBinding.toolbar) // needed to auto readjust the toolbar content color
        setStatusbarColor(color)
        setActivityToolbarColor(viewBinding.toolbar, color)

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

        cabController.cabColor = color
        activityColor = color
    }

    private val album: Album get() = model.album

    private fun load() {
        val defaultColor = primaryColor(this)
        model.loadDataSet(
            this
        ) { album, songs ->
            updateAlbumsInfo(album)
            adapter.dataset = songs
            loadImage(this)
                .from(album.safeGetFirstSong())
                .into(PaletteTargetBuilder(defaultColor)
                    .onResourceReady { result, palette ->
                        viewBinding.image.setImageDrawable(result)
                        model.paletteColor.postValue(palette)
                    }
                    .onFail {
                        viewBinding.image.setImageResource(R.drawable.default_album_art)
                        model.paletteColor.postValue(defaultColor)
                    }
                    .build())
                .enqueue()
            if (Setting.instance.isAllowedToDownloadMetadata(this)) model.loadWiki(context = this) { _, _ ->
                isWikiPreLoaded = true
            }
        }
    }

    private fun updateAlbumsInfo(album: Album) {
        supportActionBar!!.title = album.title
        viewBinding.artistText.text = album.artistName
        viewBinding.songCountText.text = songCountString(this, album.songCount)
        viewBinding.durationText.text = getReadableDurationString(album.songs.totalDuration())
        viewBinding.albumYearText.text = getYearString(album.year)
    }

    private fun setupMenu(menu: Menu) {
        albumDetailToolbar(menu, this, album, primaryTextColor(activityColor)) {
            // load wiki
            if (isWikiPreLoaded) {
                showWikiDialog()
            } else {
                model.loadWiki(this) { wikiText: Spanned?, url: String? ->
                    showWikiDialog(wikiText, url)
                }
            }
            true
        }
        tintMenuActionIcons(viewBinding.toolbar, menu, primaryTextColor(activityColor))
    }

    private var isWikiPreLoaded = false
    private val wikiDialog: MaterialDialog by lazy(LazyThreadSafetyMode.NONE) {
        MaterialDialog(this)
            .title(null, album.title)
            .message(R.string.loading)
            .positiveButton(android.R.string.ok, null, null)
            .apply {
                getActionButton(WhichButton.POSITIVE).updateTextColor(accentColor)
                getActionButton(WhichButton.NEGATIVE).updateTextColor(accentColor)
            }
    }

    private fun showWikiDialog(
        wikiText: Spanned? = model.wikiText,
        lastFMUrl: String? = model.lastFMUrl,
    ) {
        wikiDialog.show {
            if (lastFMUrl != null) {
                negativeButton(text = "Last.FM") {
                    startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(lastFMUrl)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }
            }
            if (wikiText != null) {
                message(text = wikiText)
            } else {
                message(R.string.wiki_unavailable)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TAG_EDITOR_REQUEST) {
            load()
            setResult(RESULT_OK)
        }
    }

    override fun onBackPressed() {
        if (cabController.dismiss()) return else {
            viewBinding.recyclerView.stopScroll()
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
}
