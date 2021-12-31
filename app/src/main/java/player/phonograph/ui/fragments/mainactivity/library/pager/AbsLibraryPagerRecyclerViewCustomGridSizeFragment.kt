package player.phonograph.ui.fragments.mainactivity.library.pager

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import player.phonograph.R
import player.phonograph.util.Util.isLandscape

// todo cleanup
/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Suppress("unused")
abstract class AbsLibraryPagerRecyclerViewCustomGridSizeFragment<A : RecyclerView.Adapter<*>, LM : RecyclerView.LayoutManager> :
    AbsLibraryPagerRecyclerViewFragment<A, LM>() {

    var gridSize: Int = 0
        get() {
            if (field == 0) {
                field = if (isLandscape) {
                    loadGridSizeLand()
                } else {
                    loadGridSize()
                }
            }
            return field
        }
        private set
    val maxGridSize: Int
        get() = if (isLandscape) {
            resources.getInteger(R.integer.max_columns_land)
        } else {
            resources.getInteger(R.integer.max_columns)
        }
    private val isLandscape: Boolean
        get() = isLandscape(resources)
    protected val maxGridSizeForList: Int
        get() =
            if (isLandscape) {
                requireActivity().resources.getInteger(R.integer.default_list_columns_land)
            } else requireActivity().resources.getInteger(R.integer.default_list_columns)

    var sortOrder: String? = null
        get(): String? {
            if (field == null) { sortOrder = loadSortOrder() }
            return field
        }
        private set

    private var currentLayoutRes = 0
    /**
     * Override to customize which item layout currentLayoutRes should be used. You might also want to override [.canUsePalette] then.
     * @see [gridSize]
     */
    protected val itemLayoutRes: Int
        @LayoutRes
        get() = if (gridSize > maxGridSizeForList) { R.layout.item_grid } else R.layout.item_list
    protected fun notifyLayoutResChanged(@LayoutRes res: Int) {
        currentLayoutRes = res
        val recyclerView = recyclerView
        recyclerView?.let { applyRecyclerViewPaddingForLayoutRes(it, currentLayoutRes) }
    }
    private fun applyRecyclerViewPaddingForLayoutRes(recyclerView: RecyclerView, @LayoutRes res: Int) {
        val padding: Int =
            if (res == R.layout.item_grid) { (resources.displayMetrics.density * 2).toInt() } else { 0 }
        recyclerView.setPadding(padding, padding, padding, padding)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyRecyclerViewPaddingForLayoutRes(recyclerView!!, currentLayoutRes)
    }

    private var usePaletteInitialized = false
    private var usePalette = false
    /**
     * @return whether the palette should be used at all or not
     */
    fun usePalette(): Boolean {
        if (!usePaletteInitialized) {
            usePalette = loadUsePalette()
            usePaletteInitialized = true
        }
        return usePalette
    }
    /**
     * @return whether the palette option should be available for the current item layout or not
     */
    fun canUsePalette(): Boolean {
        return itemLayoutRes == R.layout.item_grid
    }

    protected abstract fun loadGridSize(): Int
    protected abstract fun saveGridSize(gridColumns: Int)
    protected abstract fun loadGridSizeLand(): Int
    protected abstract fun saveGridSizeLand(gridColumns: Int)
    protected abstract fun saveUsePalette(usePalette: Boolean)
    protected abstract fun loadUsePalette(): Boolean
    protected abstract fun setUsePalette(usePalette: Boolean)
    protected abstract fun setGridSize(gridSize: Int)
    protected abstract fun loadSortOrder(): String?
    protected abstract fun saveSortOrder(sortOrder: String?)
    protected abstract fun setSortOrder(sortOrder: String?)

    fun setAndSaveGridSize(gridSize: Int) {
        val oldLayoutRes = itemLayoutRes
        this.gridSize = gridSize
        if (isLandscape) {
            saveGridSizeLand(gridSize)
        } else {
            saveGridSize(gridSize)
        }
        // only recreate the adapter and layout manager if the layout currentLayoutRes has changed
        if (oldLayoutRes != itemLayoutRes) {
            invalidateLayoutManager()
            invalidateAdapter()
        } else {
            setGridSize(gridSize)
        }
    }
    fun setAndSaveUsePalette(usePalette: Boolean) {
        this.usePalette = usePalette
        saveUsePalette(usePalette)
        setUsePalette(usePalette)
    }
    fun setAndSaveSortOrder(sortOrder: String?) {
        this.sortOrder = sortOrder
        saveSortOrder(sortOrder)
        setSortOrder(sortOrder)
    }
}
