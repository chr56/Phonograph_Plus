package player.phonograph.ui.modules.genre

import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.launcher.CreateFileStorageAccessDelegate
import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenDirStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDirStorageAccessDelegate
import lib.storage.launcher.OpenFileStorageAccessDelegate
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import player.phonograph.R
import player.phonograph.databinding.ActivityGenreDetailBinding
import player.phonograph.mechanism.event.EventHub
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.SongBasicDisplayPresenter
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.ui.theme.ThemeSettingsDelegate.accentColor
import player.phonograph.ui.theme.ThemeSettingsDelegate.primaryColor
import player.phonograph.ui.theme.setUpFastScrollRecyclerViewColor
import player.phonograph.ui.theme.textColorOn
import player.phonograph.ui.util.BottomViewWindowInsetsController
import player.phonograph.ui.util.applyControllableWindowInsetsAsBottomView
import player.phonograph.ui.util.menuProvider
import player.phonograph.ui.util.observe
import util.theme.view.menu.tintOverflowButtonColor
import util.theme.view.menu.tintOverflowMenuItems
import util.theme.view.menu.tintToolbarMenuActionIcons
import util.theme.view.toolbar.setToolbarColor
import androidx.activity.addCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View

class GenreDetailActivity : AbsSlidingMusicPanelActivity(),
                            ICreateFileStorageAccessible, IOpenFileStorageAccessible, IOpenDirStorageAccessible {

    private lateinit var viewBinding: ActivityGenreDetailBinding
    private val viewModel: GenreDetailActivityViewModel by viewModel { parametersOf(parseIntent(intent)) }

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()

    private lateinit var adapter: DisplayAdapter<Song>

    override fun createContentView(): View = wrapSlidingMusicPanel(viewBinding.root)

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel.loadDataSet(this)

        registerActivityResultLauncherDelegate(
            createFileStorageAccessDelegate,
            openFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
        )

        viewBinding = ActivityGenreDetailBinding.inflate(layoutInflater) // must call before super due to `createContentView()`

        super.onCreate(savedInstanceState)

        setUpToolBar()
        setUpMainContent()

        observeData()

        lifecycle.addObserver(MediaStoreListener())

        // back-press
        onBackPressedDispatcher.addCallback {
            remove()
            viewBinding.recyclerView.stopScroll()
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setUpToolBar() {
        setSupportActionBar(viewBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        viewBinding.toolbar.setNavigationOnClickListener {
            if (!isTaskRoot) onBackPressedDispatcher.onBackPressed()
        }
        viewBinding.toolbar.title = getString(R.string.label_genre)
        addMenuProvider(menuProvider(this::setupMenu))
        setToolbarColor(viewBinding.toolbar, primaryColor())
    }

    private fun setupMenu(menu: Menu) {
        val iconColor = textColorOn(this, panelViewModel.activityColor.value)
        inflateGenreDetailMenu(menu, this, viewModel.genre.value, iconColor)
        tintToolbarMenuActionIcons(menu, iconColor)
        tintOverflowButtonColor(this, iconColor)
        tintOverflowMenuItems(viewBinding.toolbar, accentColor())
    }

    private lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController
    private fun setUpMainContent() {
        adapter = DisplayAdapter(this, GenreSongDisplayPresenter)
        viewBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GenreDetailActivity)
            adapter = this@GenreDetailActivity.adapter
        }
        viewBinding.recyclerView.setUpFastScrollRecyclerViewColor(this, accentColor())
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                viewBinding.empty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            }
        })
        // WindowInsets
        bottomViewWindowInsetsController = viewBinding.recyclerView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isMiniPlayerHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }
    }

    private fun observeData() {
        observe(viewModel.genre) { genre -> viewBinding.toolbar.title = genre.name ?: "GENRE #${genre.id}" }
        observe(viewModel.songs) { songs -> adapter.dataset = songs }
    }

    private inner class MediaStoreListener : EventHub.LifeCycleEventReceiver(this, EventHub.EVENT_MUSIC_LIBRARY_CHANGED) {
        override fun onEventReceived(context: Context, intent: Intent) {
            viewModel.loadDataSet(this@GenreDetailActivity)
        }
    }

    companion object {
        private const val EXTRA_GENRE_ID = "extra_genre_id"
        fun launchIntent(from: Context, genre: Genre): Intent =
            Intent(from, GenreDetailActivity::class.java).apply {
                putExtra(EXTRA_GENRE_ID, genre.id)
            }

        private fun parseIntent(intent: Intent): Long = intent.extras?.getLong(EXTRA_GENRE_ID) ?: -1
    }

    object GenreSongDisplayPresenter : SongBasicDisplayPresenter(SortMode(SortRef.ID)) {

        override val layoutStyle: ItemLayoutStyle = ItemLayoutStyle.LIST

        override val usePalette: Boolean get() = false

        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE

    }
}
