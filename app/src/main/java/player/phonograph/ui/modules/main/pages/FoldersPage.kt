/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main.pages

import com.github.chr56.android.menu_model.MenuContext
import player.phonograph.R
import player.phonograph.mechanism.actions.ActionMenuProviders
import player.phonograph.mechanism.actions.ClickActionProviders
import player.phonograph.mechanism.actions.actionPlay
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.service.ShuffleMode
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.repo.mediastore.loaders.SongCollectionLoader
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.DisplayAdapter
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.SongBasicDisplayPresenter
import player.phonograph.ui.adapter.SongCollectionBasicDisplayPresenter
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.util.observe
import player.phonograph.util.theme.accentColor
import player.phonograph.util.ui.BottomViewWindowInsetsController
import player.phonograph.util.ui.applyControllableWindowInsetsAsBottomView
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import android.content.Context
import android.util.SparseIntArray
import android.view.View
import android.widget.ImageView
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FoldersPage : AbsPanelPage() {

    //region Content

    private val viewModel: FoldersPageViewModel by viewModels()

    override fun onPrefetchContentDataset() {
        viewModel.loadFolders(requireContext(), true)
    }

    override fun onContentChanged() {
        viewModel.loadFolders(requireContext(), false)
        viewModel.loadSongs(requireContext(), false)
    }

    override fun prepareContentView() {
        prepareRecyclerView()
        prepareAdaptersAndData()

        observe(viewLifecycleOwner.lifecycle, viewModel.bannerText) { text -> binding.panelText.text = text }

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                navigateUpBackPressedCallback.remove()
            }

            override fun onResume(owner: LifecycleOwner) {
                updateNavigateUpBackPressedCallback(viewModel.mainViewMode.value)
            }
        })
    }


    private lateinit var songCollectionDisplayAdapter: DisplayAdapter<SongCollection>
    private lateinit var songAdapter: DisplayAdapter<Song>
    private lateinit var layoutManager: GridLayoutManager

    private val panelViewModel: PanelViewModel by viewModels(ownerProducer = { requireActivity() })
    private lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController


    private fun prepareRecyclerView() {
        layoutManager = GridLayoutManager(requireContext(), folderPageDisplayConfig.gridSize)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.setUpFastScrollRecyclerViewColor(requireContext(), accentColor())

        binding.refreshContainer.apply {
            setColorSchemeColors(accentColor())
            setDistanceToTriggerSync(480)
            setProgressViewOffset(false, 10, 120)
            setOnRefreshListener {
                viewModel.loadSongs(requireContext(), false)
                viewModel.loadFolders(requireContext(), false)
                isRefreshing = false
            }
        }

        bottomViewWindowInsetsController = binding.recyclerView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isPanelHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }
    }

    private fun prepareAdaptersAndData() {

        songCollectionDisplayAdapter = DisplayAdapter(
            requireActivity(), buildFolderDisplayPresenter(layoutManager.findLastVisibleItemPosition())
        )

        songAdapter = DisplayAdapter(requireActivity(), buildSongDisplayPresenter())
        binding.recyclerView.adapter = songCollectionDisplayAdapter

        observe(viewLifecycleOwner.lifecycle, viewModel.folders) { data ->
            binding.empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
            songCollectionDisplayAdapter.dataset = data

        }
        observe(viewLifecycleOwner.lifecycle, viewModel.currentSongs) { data ->
            songAdapter.dataset = data
        }
        observe(viewLifecycleOwner.lifecycle, viewModel.mainViewMode) { mode ->
            updateNavigateUpBackPressedCallback(mode)
            binding.recyclerView.adapter = if (mode) songCollectionDisplayAdapter else songAdapter
            layoutManager.spanCount = if (mode) folderPageDisplayConfig.gridSize else songPageDisplayConfig.gridSize
            layoutManager.scrollToPosition(if (mode) viewModel.historyFolderPosition else viewModel.historyPosition)
        }
    }

    //endregion

    //region PlayerBar and Header
    override fun prepareAppBarActionButton(menuContext: MenuContext) = standardAppBarActionButton(
        menuContext = menuContext,
        onPlay = {
            lifecycleScope.launch(Dispatchers.IO) {
                val allSongs = viewModel.currentSongs.value
                if (allSongs.isNotEmpty()) allSongs.actionPlay(ShuffleMode.NONE, 0)
            }
            true
        },
        onShuffle = {
            lifecycleScope.launch(Dispatchers.IO) {
                val allSongs = viewModel.currentSongs.value
                if (allSongs.isNotEmpty()) allSongs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(allSongs.size))
            }
            true
        }
    )

    //endregion


    //region Popup

    private val folderPageDisplayConfig: PageDisplayConfig get() = FolderPageDisplayConfig(requireContext())
    private val songPageDisplayConfig: PageDisplayConfig get() = SongPageDisplayConfig(requireContext())

    override val displayConfig: PageDisplayConfig
        get() = if (viewModel.mainViewMode.value) folderPageDisplayConfig else songPageDisplayConfig

    override fun updateContentSetting(
        sortMode: SortMode,
        layout: ItemLayoutStyle,
        gridSize: Int,
        coloredFooter: Boolean,
        shouldRecreate: Boolean,
        shouldReload: Boolean,
    ) {
        if (viewModel.mainViewMode.value) {
            if (shouldReload) {
                viewModel.loadFolders(requireContext(), true)
            }
            val presenter = buildFolderDisplayPresenter(layoutManager.findLastVisibleItemPosition())
            if (shouldRecreate) {
                songCollectionDisplayAdapter = DisplayAdapter(requireActivity(), presenter)
                songCollectionDisplayAdapter.dataset = viewModel.folders.value
                binding.recyclerView.adapter = songCollectionDisplayAdapter
            } else {
                songCollectionDisplayAdapter.presenter = presenter
            }
        } else {
            if (shouldReload) {
                viewModel.loadFolders(requireContext(), false)
                viewModel.loadSongs(requireContext(), true)
            }
            val presenter = buildSongDisplayPresenter()
            if (shouldRecreate) {
                songAdapter = DisplayAdapter(requireActivity(), presenter)
                songAdapter.dataset = viewModel.currentSongs.value
                binding.recyclerView.adapter = songAdapter
            } else {
                songAdapter.presenter = presenter
            }
        }
        layoutManager.spanCount = gridSize
    }
    //endregion

    //region Navigation
    private fun updateNavigateUpBackPressedCallback(mainViewMode: Boolean) {
        if (mainViewMode || !isVisible) {
            navigateUpBackPressedCallback.remove()
        } else {
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                navigateUpBackPressedCallback
            )
        }
    }

    private val navigateUpBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigateUp()
        }
    }

    private fun navigateUp(): Boolean =
        viewModel.navigateUp(requireContext(), layoutManager.findFirstVisibleItemPosition())
    //endregion


    //region Presenters

    private fun buildSongDisplayPresenter(): DisplayPresenter<Song> =
        with(songPageDisplayConfig) { EmbedSongDisplayPresenter(sortMode, colorFooter, layout) }

    private class EmbedSongDisplayPresenter(
        sortMode: SortMode,
        override val usePalette: Boolean,
        override val layoutStyle: ItemLayoutStyle,
    ) : SongBasicDisplayPresenter(sortMode) {
        override val imageType: Int = DisplayPresenter.IMAGE_TYPE_IMAGE
    }

    private fun buildFolderDisplayPresenter(lastPosition: Int): DisplayPresenter<SongCollection> =
        FolderDisplayPresenter(folderPageDisplayConfig.sortMode, folderPageDisplayConfig.layout) { context, position ->
            viewModel.browseFolder(context, position, true, lastPosition)
            true
        }

    private class FolderDisplayPresenter(
        sortMode: SortMode,
        override val layoutStyle: ItemLayoutStyle,
        val onClick: (Context, Int) -> Boolean,
    ) : SongCollectionBasicDisplayPresenter(sortMode) {
        override val usePalette: Boolean = false

        override val clickActionProvider: ClickActionProviders.ClickActionProvider<SongCollection> =
            object : ClickActionProviders.ClickActionProvider<SongCollection> {
                override fun listClick(
                    list: List<SongCollection>,
                    position: Int,
                    context: Context,
                    imageView: ImageView?,
                ): Boolean = onClick.invoke(context, position)
            }
        override val menuProvider: ActionMenuProviders.ActionMenuProvider<SongCollection> =
            ActionMenuProviders.SongCollectionActionMenuProvider
    }
    //endregion

    class FoldersPageViewModel : ViewModel() {

        /**
         * true if browsing folders
         */
        private val _mainViewMode: MutableStateFlow<Boolean> = MutableStateFlow(true)
        val mainViewMode = _mainViewMode.asStateFlow()

        private val _folders: MutableStateFlow<List<SongCollection>> = MutableStateFlow(emptyList())
        val folders = _folders.asStateFlow()

        private val _currentSongs: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
        val currentSongs = _currentSongs.asStateFlow()

        private var _currentFolderIndex: Int = -1
        val currentFolder get() = _folders.value.getOrNull(_currentFolderIndex)

        private var _bannerText: MutableStateFlow<String> = MutableStateFlow("")
        val bannerText = _bannerText.asStateFlow()

        var historyFolderPosition: Int = 0
            private set
        private val historyPositions: SparseIntArray = SparseIntArray()
        val historyPosition: Int get() = historyPositions[_currentFolderIndex]

        fun updateBannerText(context: Context, mode: Boolean) {
            _bannerText.tryEmit(
                if (mode) {
                    context.resources.getQuantityString(
                        R.plurals.item_files, folders.value.size, folders.value.size
                    )
                } else {
                    context.resources.getQuantityString(
                        R.plurals.item_songs, currentSongs.value.size, currentSongs.value.size
                    )
                }
            )
        }

        /**
         * update folders
         */
        fun loadFolders(context: Context, updateBanner: Boolean) {
            viewModelScope.launch(Dispatchers.IO) {
                _folders.emit(
                    SongCollectionLoader.all(context = context).toMutableList().sort(context)
                )
                if (updateBanner) updateBannerText(context, true)
            }
        }

        /**
         * browse folder at [folderIndex]
         */
        fun browseFolder(context: Context, folderIndex: Int, updateBanner: Boolean, lastScrollPosition: Int) {
            viewModelScope.launch(Dispatchers.IO) {
                historyFolderPosition = lastScrollPosition
                _currentFolderIndex = folderIndex
                loadSongs(context, updateBanner)
                _mainViewMode.emit(false)
            }
        }

        /**
         * update Songs
         */
        fun loadSongs(context: Context, updateBanner: Boolean) {
            viewModelScope.launch(Dispatchers.IO) {
                _currentSongs.emit(currentFolder?.songs ?: emptyList())
                if (updateBanner) updateBannerText(context, false)
            }
        }

        /**
         * try to get back to upper level
         * @return reached top
         */
        fun navigateUp(context: Context, lastScrollPosition: Int): Boolean =
            if (mainViewMode.value) {
                true
            } else {
                _mainViewMode.value = true
                historyPositions.put(_currentFolderIndex, lastScrollPosition)
                _currentFolderIndex = -1
                _currentSongs.tryEmit(emptyList())
                updateBannerText(context, true)
                false
            }

        private fun MutableList<SongCollection>.sort(context: Context): List<SongCollection> {
            val mode = Setting(context)[Keys.collectionSortMode].data
            sortBy {
                when (mode.sortRef) {
                    SortRef.DISPLAY_NAME -> it.name
                    else                 -> null
                }
            }
            if (mode.revert) reverse()
            return this
        }
    }
}