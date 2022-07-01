package player.phonograph.ui.activities

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.bumptech.glide.Glide
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.appbar.AppBarLayout
import java.util.*
import lib.phonograph.cab.*
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.display.AlbumSongDisplayAdapter
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.databinding.ActivityAlbumDetailBinding
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.DeleteSongsDialog
import player.phonograph.dialogs.SleepTimerDialog
import player.phonograph.glide.SongGlideRequest
import player.phonograph.glide.palette.BitmapPaletteTarget
import player.phonograph.glide.palette.BitmapPaletteWrapper
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
import player.phonograph.util.PhonographColorUtil.getColor
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer
import util.mddesign.util.MaterialColorHelper
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
        Themer.setActivityToolbarColorAuto(this, viewBinding.toolbar)
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

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
            SongGlideRequest.Builder.from(Glide.with(this), album.safeGetFirstSong())
                .checkIgnoreMediaStore(this)
                .generatePalette(this).build()
                .dontAnimate()
                .into(object : BitmapPaletteTarget(viewBinding.image) {
                    val defaultColor = ThemeColor.primaryColor(this@AlbumDetailActivity)

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        model.paletteColor.postValue(defaultColor)
                    }

                    override fun onResourceReady(
                        resource: BitmapPaletteWrapper,
                        transition: Transition<in BitmapPaletteWrapper>?
                    ) {
                        super.onResourceReady(resource, transition)
                        model.paletteColor.postValue(getColor(resource.palette, defaultColor))
                    }
                })
            if (isAllowedToDownloadMetadata(this)) model.loadWiki(context = this)
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
                if (model.wikiDialog == null) {
                    model.wikiDialog = MaterialDialog(this)
                        .title(null, album.title)
                        .positiveButton(android.R.string.ok, null, null)
                        .apply {
                            getActionButton(WhichButton.POSITIVE).updateTextColor(accentColor)
                        }
                }
                if (isAllowedToDownloadMetadata(this)) {
                    model.wiki?.let { wiki ->
                        with(model.wikiDialog!!) {
                            message(null, wiki, null)
                            show()
                        }
                    } ?: Toast.makeText(this, resources.getString(R.string.wiki_unavailable), Toast.LENGTH_SHORT).show()
                } else {
                    model.wikiDialog!!.show()
                    model.loadWiki(this)
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
        setLightStatusbar(false)
    }
}
