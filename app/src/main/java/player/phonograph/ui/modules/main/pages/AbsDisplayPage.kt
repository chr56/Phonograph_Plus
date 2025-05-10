/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import com.github.chr56.android.menu_model.MenuContext
import player.phonograph.R
import player.phonograph.mechanism.actions.actionPlay
import player.phonograph.model.Song
import player.phonograph.model.service.ShuffleMode
import player.phonograph.model.sort.SortMode
import player.phonograph.model.ui.ItemLayoutStyle
import player.phonograph.ui.modules.panel.PanelViewModel
import player.phonograph.util.observe
import player.phonograph.util.theme.accentColor
import player.phonograph.util.ui.BottomViewWindowInsetsController
import player.phonograph.util.ui.applyControllableWindowInsetsAsBottomView
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.view.View
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AbsDisplayPage<IT, A : RecyclerView.Adapter<*>> : AbsPanelPage() {

    //region Content

    protected abstract val viewModel: AbsDisplayPageViewModel<IT>

    override fun onPrefetchContentDataset() {
        viewModel.loadDataset(requireContext())
    }

    override fun onContentChanged() {
        viewModel.loadDataset(requireContext())
    }

    override fun prepareContentView() {
        prepareRecyclerView()
        prepareAdapter()
    }

    protected val panelViewModel: PanelViewModel by viewModels(ownerProducer = { requireActivity() })
    protected lateinit var bottomViewWindowInsetsController: BottomViewWindowInsetsController

    private fun prepareRecyclerView() {
        val context = requireContext()
        layoutManager = GridLayoutManager(requireContext(), displayConfig.gridSize)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.setUpFastScrollRecyclerViewColor(context, accentColor())

        binding.refreshContainer.apply {
            setColorSchemeColors(accentColor())
            setDistanceToTriggerSync(480)
            setProgressViewOffset(false, 10, 120)
            setOnRefreshListener { viewModel.loadDataset(context) }
        }
        observe(viewModel.loading) { binding.refreshContainer.isRefreshing = it }

        bottomViewWindowInsetsController = binding.recyclerView.applyControllableWindowInsetsAsBottomView()
        observe(panelViewModel.isPanelHidden) { hidden -> bottomViewWindowInsetsController.enabled = hidden }
    }

    protected lateinit var adapter: A
    protected lateinit var layoutManager: GridLayoutManager
    private fun prepareAdapter() {
        adapter = createAdapter()
        binding.recyclerView.adapter = adapter
        observe(viewLifecycleOwner.lifecycle, viewModel.dataset) { items ->
            updateDisplayedItems(items.toList())

            binding.empty.text = resources.getText(R.string.empty)
            binding.empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

            val headerTextRes = viewModel.headerTextRes
            if (headerTextRes != 0) {
                binding.panelText.text = resources.getQuantityString(headerTextRes, items.size, items.size)
            }
        }
    }

    protected abstract fun createAdapter(): A

    protected abstract fun updateDisplayedItems(items: List<IT>)
    //endregion


    //region PlayerBar and Header
    override fun prepareAppBarActionButton(menuContext: MenuContext) = standardAppBarActionButton(
        menuContext = menuContext,
        onPlay = {
            lifecycleScope.launch(Dispatchers.IO) {
                val allSongs = viewModel.collectAllSongs(menuContext.context)
                allSongs.actionPlay(ShuffleMode.NONE, 0)
            }
            true
        },
        onShuffle = {
            lifecycleScope.launch(Dispatchers.IO) {
                val allSongs = viewModel.collectAllSongs(menuContext.context)
                allSongs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(allSongs.size))
            }
            true
        }
    )

    //endregion

    //region Popup
    override fun updateContentSetting(
        sortMode: SortMode,
        layout: ItemLayoutStyle,
        gridSize: Int,
        coloredFooter: Boolean,
        shouldRecreate: Boolean,
        shouldReload: Boolean,
    ) {
        if (shouldReload) {
            viewModel.loadDataset(requireContext())
        }
        if (shouldRecreate) {
            adapter = createAdapter()
            binding.recyclerView.adapter = adapter
            updateDisplayedItems(viewModel.dataset.value.toList())
        } else {
            updatePresenterSettings(sortMode, coloredFooter, layout)
        }
        layoutManager.spanCount = gridSize
    }

    protected abstract fun updatePresenterSettings(
        sortMode: SortMode,
        usePalette: Boolean,
        layoutStyle: ItemLayoutStyle,
    )
    //endregion

    abstract class AbsDisplayPageViewModel<IT> : ViewModel() {

        private val _dataset: MutableStateFlow<Collection<IT>> = MutableStateFlow(emptyList())
        val dataset: StateFlow<Collection<IT>> get() = _dataset.asStateFlow()

        private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val loading get() = _loading.asStateFlow()

        private var job: Job? = null
        fun loadDataset(context: Context) {
            job?.cancel()
            job = viewModelScope.launch(Dispatchers.IO) {
                _loading.value = true
                val items = loadDataSetImpl(context, this)
                _dataset.emit(items)
                _loading.value = false
            }
        }

        abstract suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<IT>

        /**
         * @return all songs on this page
         */
        abstract suspend fun collectAllSongs(context: Context): List<Song>

        abstract val headerTextRes: Int

    }
}