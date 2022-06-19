package player.phonograph.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
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
import com.google.android.material.appbar.AppBarLayout
import lib.phonograph.cab.*
import player.phonograph.R
import player.phonograph.adapter.display.AlbumSongDisplayAdapter
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.databinding.ActivityAlbumDetailBinding
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.DeleteSongsDialog
import player.phonograph.dialogs.SleepTimerDialog
import player.phonograph.glide.PhonographColoredTarget
import player.phonograph.glide.SongGlideRequest
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.model.Album
import player.phonograph.service.MusicPlayerRemote.enqueue
import player.phonograph.service.MusicPlayerRemote.openAndShuffleQueue
import player.phonograph.service.MusicPlayerRemote.playNext
import player.phonograph.settings.Setting.Companion.isAllowedToDownloadMetadata
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.MusicUtil.getReadableDurationString
import player.phonograph.util.MusicUtil.getSongCountString
import player.phonograph.util.MusicUtil.getTotalDuration
import player.phonograph.util.MusicUtil.getYearString
import player.phonograph.util.NavigationUtil.goToArtist
import player.phonograph.util.NavigationUtil.openEqualizer
import player.phonograph.util.PhonographColorUtil.shiftBackgroundColorForLightText
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer
import util.mddesign.util.MaterialColorHelper
import util.mddesign.util.Util
import util.phonograph.lastfm.rest.LastFMRestClient
import util.phonograph.lastfm.rest.model.LastFmAlbum
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.AlbumTagEditorActivity
import java.util.*

/**
 * Be careful when changing things in this Activity!
 */
class AlbumDetailActivity : AbsSlidingMusicPanelActivity(), MultiSelectionCabProvider {

    companion object {
        private const val TAG_EDITOR_REQUEST = 2001
        const val EXTRA_ALBUM_ID = "extra_album_id"
    }

    private lateinit var viewBinding: ActivityAlbumDetailBinding
    private val model: AlbumDetailActivityViewModel by viewModels()

    private lateinit var adapter: SongDisplayAdapter

    private val lastFMRestClient: LastFMRestClient by lazy { LastFMRestClient(this) }
    private var wiki: Spanned? = null
    private var wikiDialog: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val albumID = intent.extras!!.getLong(EXTRA_ALBUM_ID)
        model.albumId = albumID
        load()

        viewBinding = ActivityAlbumDetailBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)

        // activity
        setDrawUnderStatusbar()
        Themer.setActivityToolbarColorAuto(this, viewBinding.toolbar)
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // content
        setUpViews()
    }

    override fun createContentView(): View = wrapSlidingMusicPanel(viewBinding.root)

    private fun setUpViews() {
        viewBinding.innerAppBar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                viewBinding.recyclerView.setPaddingTop(viewBinding.innerAppBar.totalScrollRange + verticalOffset)
            }
        )
        viewBinding.recyclerView.setPaddingTop(viewBinding.innerAppBar.totalScrollRange)
        // setUpSongsAdapter
        adapter = AlbumSongDisplayAdapter(this, this, album.songs, R.layout.item_list) {
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
            goToArtist(this@AlbumDetailActivity, album.artistId)
        }
        // paletteColor
        model.paletteColor.observe(this) {
            updateColors(it)
        }
        model.paletteColor.value = Util.resolveColor(this, R.attr.defaultFooterColor)
    }

    private fun RecyclerView.setPaddingTop(top: Int) = setPadding(paddingLeft, top, paddingRight, paddingBottom)

    private fun updateColors(color: Int) {
        viewBinding.recyclerView.setUpFastScrollRecyclerViewColor(this, color)
        viewBinding.header.setBackgroundColor(color)
        setNavigationbarColor(color)
        setTaskDescriptionColor(color)
        viewBinding.toolbar.setBackgroundColor(color)
        setSupportActionBar(viewBinding.toolbar) // needed to auto readjust the toolbar content color
        setStatusbarColor(color)
        val secondaryTextColor = MaterialColorHelper.getSecondaryTextColor(this, ColorUtil.isColorLight(color))

        val artistIcon = getTintedDrawable(R.drawable.ic_person_white_24dp, secondaryTextColor)!!
        viewBinding.artistText.setCompoundDrawablesWithIntrinsicBounds(artistIcon, null, null, null)
        viewBinding.artistText.setTextColor(MaterialColorHelper.getPrimaryTextColor(this, ColorUtil.isColorLight(color)))
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
    }

    private val album: Album get() = model.album

    private fun load() {
        model.loadDataSet(
            this
        ) { album, songs ->
            updateAlbumsInfo(album)
            adapter.dataset = songs
            SongGlideRequest.Builder.from(Glide.with(this), album.safeGetFirstSong())
                .checkIgnoreMediaStore(this)
                .generatePalette(this).build()
                .dontAnimate()
                .into(object : PhonographColoredTarget(viewBinding.image) {
                    override fun onColorReady(color: Int) {
                        model.paletteColor.postValue(color)
                    }
                })
            if (isAllowedToDownloadMetadata(this)) loadWiki()
        }
    }

    private fun updateAlbumsInfo(album: Album) {
        supportActionBar!!.title = album.title
        viewBinding.artistText.text = album.artistName
        viewBinding.songCountText.text = getSongCountString(this, album.songCount)
        viewBinding.durationText.text = getReadableDurationString(getTotalDuration(this, album.songs))
        viewBinding.albumYearText.text = getYearString(album.year)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_album_detail, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun loadWiki(lang: String? = Locale.getDefault().language) {
        wiki = null
        lastFMRestClient.apiService
            .getAlbumInfo(album.title, album.artistName, lang)
            .enqueue(object : Callback<LastFmAlbum?> {
                override fun onResponse(call: Call<LastFmAlbum?>, response: Response<LastFmAlbum?>) {
                    val lastFmAlbum = response.body()
                    if (lastFmAlbum != null && lastFmAlbum.album != null && lastFmAlbum.album.wiki != null) {
                        val wikiContent = lastFmAlbum.album.wiki.content
                        if (wikiContent != null && wikiContent.trim { it <= ' ' }.isNotEmpty()) {
                            wiki = Html.fromHtml(wikiContent, Html.FROM_HTML_MODE_LEGACY)
                        }
                    }

                    // If the "lang" parameter is set and no wiki is given, retry with default language
                    if (wiki == null && lang != null) {
                        loadWiki(null)
                        return
                    }
                    if (!isAllowedToDownloadMetadata(this@AlbumDetailActivity)) {
                        if (wiki != null) {
                            wikiDialog!!.message(null, wiki, null)
                        } else {
                            wikiDialog!!.dismiss()
                            Toast.makeText(this@AlbumDetailActivity, resources.getString(R.string.wiki_unavailable), Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }

                override fun onFailure(call: Call<LastFmAlbum?>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sleep_timer -> {
                SleepTimerDialog().show(supportFragmentManager, "SET_SLEEP_TIMER")
                return true
            }
            R.id.action_equalizer -> {
                openEqualizer(this)
                return true
            }
            R.id.action_shuffle_album -> {
                openAndShuffleQueue(adapter.dataset, true)
                return true
            }
            R.id.action_play_next -> {
                playNext(adapter.dataset)
                return true
            }
            R.id.action_add_to_current_playing -> {
                enqueue(adapter.dataset)
                return true
            }
            R.id.action_add_to_playlist -> {
                AddToPlaylistDialog.create(adapter.dataset).show(supportFragmentManager, "ADD_PLAYLIST")
                return true
            }
            R.id.action_delete_from_device -> {
                DeleteSongsDialog.create(adapter.dataset).show(supportFragmentManager, "DELETE_SONGS")
                return true
            }
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
            R.id.action_tag_editor -> {
                val intent = Intent(this, AlbumTagEditorActivity::class.java)
                intent.putExtra(AbsTagEditorActivity.EXTRA_ID, album.id)
                startActivityForResult(intent, TAG_EDITOR_REQUEST)
                return true
            }
            R.id.action_go_to_artist -> {
                goToArtist(this, album.artistId)
                return true
            }
            R.id.action_wiki -> {
                if (wikiDialog == null) {
                    wikiDialog = MaterialDialog(this)
                        .title(null, album.title)
                        .positiveButton(android.R.string.ok, null, null)
                    // set button color
                    wikiDialog!!.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(this))
                }
                if (isAllowedToDownloadMetadata(this)) {
                    if (wiki != null) {
                        wikiDialog!!.message(null, wiki, null)
                        wikiDialog!!.show()
                    } else {
                        Toast.makeText(this, resources.getString(R.string.wiki_unavailable), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    wikiDialog!!.show()
                    loadWiki()
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TAG_EDITOR_REQUEST) {
            load()
            setResult(RESULT_OK)
        }
    }

    private var multiSelectionCab: MultiSelectionCab? = null
    override fun getCab(): MultiSelectionCab? = multiSelectionCab

    override fun deployCab(
        menuRes: Int,
        initCallback: InitCallback?,
        showCallback: ShowCallback?,
        selectCallback: SelectCallback?,
        hideCallback: HideCallback?,
        destroyCallback: DestroyCallback?,
    ): MultiSelectionCab {
        val cfg: CabCfg = {
            val textColor = Color.BLACK

            backgroundColor = shiftBackgroundColorForLightText(model.paletteColor.value!!)
            titleTextColor = textColor

            closeDrawable = AppCompatResources.getDrawable(this@AlbumDetailActivity, R.drawable.ic_close_white_24dp)!!.also {
                it.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(textColor, BlendModeCompat.SRC_IN)
            }

            this.menuRes = menuRes

            onInit(initCallback)
            onShow(showCallback)
            onSelection(selectCallback)
            onHide(hideCallback)
            onClose { dismissCab() }
            onDestroy(destroyCallback)
        }
        if (multiSelectionCab == null) multiSelectionCab =
            createMultiSelectionCab(this@AlbumDetailActivity, R.id.cab_stub, R.id.multi_selection_cab, cfg)
        else {
            multiSelectionCab!!.applyCfg = cfg
            multiSelectionCab!!.refresh()
        }

        return multiSelectionCab!!
    }

    override fun showCab() {
        multiSelectionCab?.let { cab ->
            viewBinding.toolbar.visibility = View.INVISIBLE
            cab.refresh()
            cab.show()
        }
    }

    override fun dismissCab() {
        multiSelectionCab?.hide()
        viewBinding.toolbar.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        if (multiSelectionCab != null && multiSelectionCab!!.status == CabStatus.STATUS_ACTIVE) {
            dismissCab()
        } else {
            viewBinding.recyclerView.stopScroll()
            finish()
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
}
