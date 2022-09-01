package player.phonograph.ui.activities

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import kotlinx.coroutines.*
import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.createToolbarCab
import mt.tint.setActivityToolbarColorAuto
import mt.util.color.primaryTextColor
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.databinding.ActivityGenreDetailBinding
import player.phonograph.mediastore.GenreLoader
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor

class GenreDetailActivity :
    AbsSlidingMusicPanelActivity() {

    private var _viewBinding: ActivityGenreDetailBinding? = null
    private val binding: ActivityGenreDetailBinding get() = _viewBinding!!

    private lateinit var genre: Genre
    private lateinit var adapter: SongDisplayAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        genre = intent.extras?.getParcelable(EXTRA_GENRE) ?: throw Exception(
            "No genre in the intent!"
        )
        loadDataSet(this)
        _viewBinding = ActivityGenreDetailBinding.inflate(layoutInflater)

        super.onCreate(savedInstanceState)

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
                if (isRecyclerViewPrepared) adapter.dataset = list
            }
        }
    }

    private fun setUpRecyclerView() {
        adapter =
            SongDisplayAdapter(this, cabController, ArrayList(), R.layout.item_list) {
                showSectionName = false
            }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GenreDetailActivity)
            adapter = this@GenreDetailActivity.adapter
        }
        binding.recyclerView.setUpFastScrollRecyclerViewColor(this, accentColor)
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
        isRecyclerViewPrepared = true
    }

    private fun setUpToolBar() {
        binding.toolbar.setBackgroundColor(primaryColor)
        setSupportActionBar(binding.toolbar)
        setActivityToolbarColorAuto(binding.toolbar)
        supportActionBar!!.title = genre.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        cab = createToolbarCab(this, R.id.cab_stub, R.id.multi_selection_cab)
        cabController = MultiSelectionCabController(cab)
    }

    lateinit var cab: ToolbarCab
    lateinit var cabController: MultiSelectionCabController

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val iconColor = primaryTextColor(primaryColor)
        attach(menu) {
            menuItem {
                title = getString(R.string.action_shuffle_playlist)
                icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    MusicPlayerRemote
                        .playQueueCautiously(adapter.dataset, 0, true, ShuffleMode.SHUFFLE)
                    true
                }
            }

            menuItem {
                title = getString(R.string.action_play)
                icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    MusicPlayerRemote
                        .playQueueCautiously(adapter.dataset, 0, true, ShuffleMode.NONE)
                    true
                }
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (cabController.dismiss()) return else super.onBackPressed()
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

    /* *******************
     *
     *     cabCallBack
     *
     * *******************/

    companion object {
        const val EXTRA_GENRE = "extra_genre"
    }
}
