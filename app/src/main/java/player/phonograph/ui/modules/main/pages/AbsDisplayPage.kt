/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import com.github.chr56.android.menu_model.MenuContext
import player.phonograph.R
import player.phonograph.mechanism.actions.actionPlay
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.Song
import player.phonograph.model.sort.SortMode
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.util.theme.accentColor
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.loading.collect {
                    binding.refreshContainer.isRefreshing = it
                }
            }
        }
    }

    protected lateinit var adapter: A
    protected lateinit var layoutManager: GridLayoutManager
    private fun prepareAdapter() {
        adapter = createAdapter()
        binding.recyclerView.adapter = adapter
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.dataSet.collect { items ->
                    updateDisplayedItems(items.toList())
                    updateHeaderText(viewModel.headerText(requireContext()))
                    binding.empty.text = resources.getText(R.string.empty)
                    binding.empty.visibility = if (viewModel.isEmpty) View.VISIBLE else View.GONE
                }
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
            updateDisplayedItems(viewModel.dataSet.value.toList())
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

        private val _dataSet: MutableStateFlow<Collection<IT>> = MutableStateFlow(emptyList())
        val dataSet: StateFlow<Collection<IT>> get() = _dataSet.asStateFlow()

        private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val loading get() = _loading.asStateFlow()

        val isEmpty get() = dataSet.value.isEmpty()
        // val isEmptyFlow: Flow<Boolean> = _dataSet.map { it.isEmpty() }

        private var job: Job? = null
        fun loadDataset(context: Context) {
            job?.cancel()
            job = viewModelScope.launch(Dispatchers.IO) {
                _loading.value = true
                val items = loadDataSetImpl(context, this)
                _dataSet.emit(items)
                _loading.value = false
            }
        }

        abstract suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<IT>

        /**
         * @return all songs on this page
         */
        abstract suspend fun collectAllSongs(context: Context): List<Song>

        abstract val headerTextRes: Int
        fun headerText(context: Context): CharSequence? {
            if (headerTextRes <= 0) return null
            val n = dataSet.value.size
            return context.resources.getQuantityString(headerTextRes, n, n)
        }

    }
}