package player.phonograph.ui.activities

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Spanned
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.google.android.material.appbar.AppBarLayout
import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.createToolbarCab
import mt.pref.ThemeColor.primaryColor
import mt.tint.requireLightStatusbar
import mt.tint.setActivityToolbarColorAuto
import mt.tint.setNavigationBarColor
import mt.tint.viewtint.tintMenu
import mt.util.color.getPrimaryTextColor
import mt.util.color.isColorLight
import mt.util.color.primaryTextColor
import mt.util.color.secondaryTextColor
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.display.AlbumSongDisplayAdapter
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PhonographColoredTarget
import player.phonograph.databinding.ActivityAlbumDetailBinding
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.DeleteSongsDialog
import player.phonograph.dialogs.SleepTimerDialog
import player.phonograph.model.Album
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.getYearString
import player.phonograph.model.songCountString
import player.phonograph.model.totalDuration
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicPlayerRemote.enqueue
import player.phonograph.service.MusicPlayerRemote.playNext
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Setting.Companion.isAllowedToDownloadMetadata
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.NavigationUtil.goToArtist
import player.phonograph.util.NavigationUtil.openEqualizer
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor
import util.phonograph.tageditor.AbsTagEditorActivity
import util.phonograph.tageditor.AlbumTagEditorActivity

/**
 * Be careful when changing things in this Activity!
 */
class AlbumDetailActivity : AbsSlidingMusicPanelActivity() {

    companion object {
        private const val TAG_EDITOR_REQUEST = 2001
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
            goToArtist(this@AlbumDetailActivity, album.artistId)
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
        setActivityToolbarColorAuto(viewBinding.toolbar)

        val secondaryTextColor = secondaryTextColor(color)

        val artistIcon = getTintedDrawable(R.drawable.ic_person_white_24dp, secondaryTextColor)!!
        viewBinding.artistText.setCompoundDrawablesWithIntrinsicBounds(artistIcon, null, null, null)
        viewBinding.artistText.setTextColor(getPrimaryTextColor(this, isColorLight(color)))
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
        model.loadDataSet(
            this
        ) { album, songs ->
            updateAlbumsInfo(album)
            adapter.dataset = songs
            loadImage(this)
                .from(album.safeGetFirstSong())
                .into(object : PhonographColoredTarget() {
                    override fun onResourcesReady(drawable: Drawable) {
                        viewBinding.image.setImageDrawable(drawable)
                    }

                    override fun onColorReady(color: Int) {
                        model.paletteColor.postValue(color)
                    }

                    val defaultColor = primaryColor(this@AlbumDetailActivity)
                    override fun onError(error: Drawable?) {
                        viewBinding.image.setImageResource(R.drawable.default_album_art)
                        model.paletteColor.postValue(defaultColor)
                    }
                })
                .enqueue()
            if (isAllowedToDownloadMetadata(this)) model.loadWiki(context = this) { _, _ ->
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_album_detail, menu)
        viewBinding.toolbar.apply {
            tintMenu(this, menu, primaryTextColor(activityColor))
        }
        return super.onCreateOptionsMenu(menu)
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
                MusicPlayerRemote
                    .playQueue(adapter.dataset, 0, true, ShuffleMode.SHUFFLE)
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
                DeleteSongsDialog.create(ArrayList(adapter.dataset)).show(supportFragmentManager, "DELETE_SONGS")
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
                if (isWikiPreLoaded) {
                    showWikiDialog()
                } else {
                    model.loadWiki(this) { wikiText: Spanned?, url: String? ->
                        showWikiDialog(wikiText, url)
                    }
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
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
