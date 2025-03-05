/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.main.pages

import com.github.chr56.android.menu_model.MenuContext
import player.phonograph.mechanism.actions.ActionMenuProviders
import player.phonograph.mechanism.actions.ClickActionProviders
import player.phonograph.mechanism.actions.actionPlay
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.sort.SortMode
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.adapter.DisplayPresenter
import player.phonograph.ui.adapter.GenericDisplayAdapter
import player.phonograph.ui.adapter.SongBasicDisplayPresenter
import player.phonograph.ui.adapter.SongCollectionBasicDisplayPresenter
import player.phonograph.util.theme.accentColor
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import android.content.Context
import android.view.View
import android.widget.ImageView
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NeoFlattenFolderPage : PanelDisplayPage() {

    //region Content

    private val viewModel: FlattenFolderPageViewModel by viewModels()

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

        lifecycleScope.launch {
            viewModel.bannerText.collect { text ->
                binding.panelText.text = text
            }
        }

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                navigateUpBackPressedCallback.remove()
            }

            override fun onResume(owner: LifecycleOwner) {
                updateNavigateUpBackPressedCallback(viewModel.mainViewMode.value)
            }
        })
    }


    private lateinit var songCollectionDisplayAdapter: GenericDisplayAdapter<SongCollection>
    private lateinit var songAdapter: GenericDisplayAdapter<Song>
    private lateinit var layoutManager: GridLayoutManager


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
    }

    private fun prepareAdaptersAndData() {

        songCollectionDisplayAdapter = GenericDisplayAdapter(
            requireActivity(), buildFolderDisplayPresenter(layoutManager.findLastVisibleItemPosition())
        )

        songAdapter = GenericDisplayAdapter(requireActivity(), buildSongDisplayPresenter())
        binding.recyclerView.adapter = songCollectionDisplayAdapter

        lifecycleScope.launch {
            viewModel.folders.collect { data ->
                binding.empty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
                songCollectionDisplayAdapter.dataset = data
            }
        }
        lifecycleScope.launch {
            viewModel.currentSongs.collect { data ->
                songAdapter.dataset = data
            }
        }
        lifecycleScope.launch {
            viewModel.mainViewMode.collect { mode ->
                updateNavigateUpBackPressedCallback(mode)
                binding.recyclerView.adapter = if (mode) songCollectionDisplayAdapter else songAdapter
                layoutManager.spanCount = if (mode) folderPageDisplayConfig.gridSize else songPageDisplayConfig.gridSize
                layoutManager.scrollToPosition(if (mode) viewModel.historyFolderPosition else viewModel.historyPosition)
            }
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
                songCollectionDisplayAdapter = GenericDisplayAdapter(requireActivity(), presenter)
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
                songAdapter = GenericDisplayAdapter(requireActivity(), presenter)
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
}