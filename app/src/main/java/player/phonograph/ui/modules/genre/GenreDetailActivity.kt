package player.phonograph.ui.modules.genre

import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.launcher.CreateFileStorageAccessDelegate
import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenDirStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDirStorageAccessDelegate
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.databinding.ActivityGenreDetailBinding
import player.phonograph.foundation.compat.parcelable
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.actions.DetailToolbarMenuProviders
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.SongBasicDisplayPresenter
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.util.observe
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.ui.BottomViewWindowInsetsController
import player.phonograph.util.ui.applyControllableWindowInsetsAsBottomView
import player.phonograph.util.ui.menuProvider
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import util.theme.color.primaryTextColor
import util.theme.view.menu.tintOverflowButtonColor
import util.theme.view.menu.tintToolbarMenuActionIcons
import util.theme.view.toolbar.setToolbarColor
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class GenreDetailActivity : AbsSlidingMusicPanelActivity(),
                            ICreateFileStorageAccessible, IOpenFileStorageAccessible, IOpenDirStorageAccessible {

    private var _viewBinding: ActivityGenreDetailBinding? = null
    private val binding: ActivityGenreDetailBinding get() = _viewBinding!!

    private lateinit var genre: Genre
    private lateinit var adapter: DisplayAdapter<Song>

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()

    private lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController

    override fun onCreate(savedInstanceState: Bundle?) {
        genre = parseIntent(intent) ?: throw IllegalArgumentException()
        loadDataSet(this)
        _viewBinding = ActivityGenreDetailBinding.inflate(layoutInflater)

        registerActivityResultLauncherDelegate(
            createFileStorageAccessDelegate,
            openFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
        )

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
        lifecycleScope.launch(Dispatchers.IO) {
            val list: List<Song> = Songs.genres(context, genre.id)

            while (!isRecyclerViewPrepared) yield() // wait until ready

            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) adapter.dataset = list
            }
        }
    }

    private fun setUpRecyclerView() {
        adapter = DisplayAdapter(this, GenreSongDisplayPresenter)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GenreDetailActivity)
            adapter = this@GenreDetailActivity.adapter
        }
        binding.recyclerView.setUpFastScrollRecyclerViewColor(this, accentColor())
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
        isRecyclerViewPrepared = true
        // WindowInsets
        bottomViewWindowInsetsController = binding.recyclerView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isPanelHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }
    }

    private fun setUpToolBar() {
        binding.toolbar.setBackgroundColor(primaryColor())
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.title = genre.name
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setToolbarColor(binding.toolbar, primaryColor())
    }

    private fun setupMenu(menu: Menu) {
        val iconColor = primaryTextColor(panelViewModel.activityColor.value)
        DetailToolbarMenuProviders.GenreEntityToolbarMenuProvider.inflateMenu(menu, this, genre, iconColor)
        tintToolbarMenuActionIcons(menu, iconColor)
        tintOverflowButtonColor(this, iconColor)
    }


    private inner class MediaStoreListener : EventHub.LifeCycleEventReceiver(this, EventHub.EVENT_MEDIASTORE_CHANGED) {
        override fun onEventReceived(context: Context, intent: Intent) {
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

    companion object {
        private const val EXTRA_GENRE = "extra_genre"
        fun launchIntent(from: Context, genre: Genre): Intent =
            Intent(from, GenreDetailActivity::class.java).apply {
                putExtra(EXTRA_GENRE, genre)
            }

        private fun parseIntent(intent: Intent) = intent.extras?.parcelable<Genre>(EXTRA_GENRE)
    }

    object GenreSongDisplayPresenter : SongBasicDisplayPresenter(SortMode(SortRef.ID)) {

        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST

        override val usePalette: Boolean get() = false

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE

    }
}
