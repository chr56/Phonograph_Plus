package player.phonograph.ui.activities

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialcab.CreateCallback
import com.afollestad.materialcab.DestroyCallback
import com.afollestad.materialcab.SelectCallback
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import kotlinx.coroutines.*
import player.phonograph.R
import player.phonograph.adapter.song.SongAdapter
import player.phonograph.databinding.ActivityGenreDetailBinding
import player.phonograph.interfaces.CabHolder
import player.phonograph.loader.GenreLoader
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.PhonographColorUtil
import player.phonograph.util.ViewUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer

class GenreDetailActivity :
    AbsSlidingMusicPanelActivity(), CabHolder {

    private var _viewBinding: ActivityGenreDetailBinding? = null
    private val binding: ActivityGenreDetailBinding get() = _viewBinding!!

    private lateinit var genre: Genre
    private lateinit var adapter: SongAdapter

    private var cab: AttachedCab? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        genre = intent.extras?.getParcelable(EXTRA_GENRE) ?: throw Exception("No genre in the intent!")
        loadDataSet(this)
        _viewBinding = ActivityGenreDetailBinding.inflate(layoutInflater)

        super.onCreate(savedInstanceState)

        setDrawUnderStatusbar()
        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        setUpToolBar()
        setUpRecyclerView()
    }

    override fun createContentView(): View {
        return wrapSlidingMusicPanel(binding.root)
    }

    private val loaderCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private var isRecyclerViewPrepared: Boolean = false

    private fun loadDataSet(context: Context) {
        loaderCoroutineScope.launch {

            val list: List<Song> = GenreLoader.getSongs(context, genre.id)

            while (!isRecyclerViewPrepared) yield() // wait until ready

            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) adapter.dataSet = list
            }
        }
    }

    private fun setUpRecyclerView() {
        adapter = SongAdapter(this, ArrayList(), R.layout.item_list, false, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GenreDetailActivity)
            adapter = this@GenreDetailActivity.adapter
        }
        ViewUtil.setUpFastScrollRecyclerViewColor(this, binding.recyclerView, ThemeColor.accentColor(this))
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
        isRecyclerViewPrepared = true
    }

    private fun setUpToolBar() {
        binding.toolbar.setBackgroundColor(ThemeColor.primaryColor(this))
        Themer.setActivityToolbarColorAuto(this, binding.toolbar)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.title = genre.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_genre_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_shuffle_genre -> {
                MusicPlayerRemote.openAndShuffleQueue(adapter.dataSet, true)
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showCab(
        menuRes: Int,
        createCallback: CreateCallback,
        selectCallback: SelectCallback,
        destroyCallback: DestroyCallback
    ): AttachedCab {

        cab?.let {
            if (it.isActive()) it.destroy()
        }
        cab = createCab(R.id.cab_stub) {
            menu(menuRes)
            popupTheme(Setting.instance.generalTheme)
            closeDrawable(R.drawable.ic_close_white_24dp)
            backgroundColor(literal = PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(this@GenreDetailActivity)))
            onCreate(createCallback)
            onSelection(selectCallback)
            onDestroy(destroyCallback)
        }

        return cab as AttachedCab
    }

    override fun onBackPressed() {
        if (cab != null && cab.isActive()) cab.destroy() else {
            binding.recyclerView.stopScroll()
            super.onBackPressed()
        }
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        loadDataSet(this)
    }

    private fun checkIsEmpty() {
        binding.empty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        binding.recyclerView.adapter = null
        super.onDestroy()
        loaderCoroutineScope.cancel()
        _viewBinding = null
    }

    companion object {
        const val EXTRA_GENRE = "extra_genre"
    }
}
