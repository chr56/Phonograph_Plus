package player.phonograph.ui.activities

import lib.phonograph.cab.ToolbarCab
import lib.phonograph.cab.createToolbarCab
import lib.phonograph.misc.menuProvider
import mt.tint.setActivityToolbarColorAuto
import player.phonograph.R
import player.phonograph.actions.menu.genreDetailToolbar
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.adapter.display.SongDisplayAdapter
import player.phonograph.databinding.ActivityGenreDetailBinding
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.GenreLoader
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

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

        lifecycle.addObserver(MediaStoreListener())
    }

    override fun createContentView(): View {
        return wrapSlidingMusicPanel(binding.root)
    }

    private var isRecyclerViewPrepared: Boolean = false

    private fun loadDataSet(context: Context) {
        lifecycleScope.launch {
            val list: List<Song> = GenreLoader.genreSongs(context, genre.id)

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
        supportActionBar!!.title = genre.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setActivityToolbarColorAuto(binding.toolbar)

        cab = createToolbarCab(this, R.id.cab_stub, R.id.multi_selection_cab)
        cabController = MultiSelectionCabController(cab)
    }

    lateinit var cab: ToolbarCab
    lateinit var cabController: MultiSelectionCabController

    private fun setupMenu(menu: Menu) {
        genreDetailToolbar(menu, this, genre)
    }


    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() {
            loadDataSet(this@GenreDetailActivity)
        }
    }

    private fun checkIsEmpty() {
        binding.empty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        binding.recyclerView.adapter = null
        super.onDestroy()
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
