/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.home

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.model.Displayable
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.Util
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor


/**
 * @param IT the model type that this fragment displays
 * @param A relevant Adapter
 * @param LM relevant LayoutManager
 */
sealed class AbsDisplayPage<IT, A : DisplayAdapter<out Displayable>, LM : GridLayoutManager> :
    AbsPage() {

    private var _viewBinding: FragmentDisplayPageBinding? = null
    private val binding get() = _viewBinding!!

    abstract fun getDataSet(): List<IT>
    abstract fun loadDataSet()

    /**
     * Notify every [Displayable] items changes, do not reload dataset
     */
    abstract fun refreshDataSet()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        loadDataSet()
        _viewBinding = FragmentDisplayPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    // for mini player bar
    private var outerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.container.paddingTop,
                binding.container.paddingRight,
                hostFragment.totalAppBarScrollingRange + verticalOffset
            )
        }

    private var innerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.banner.totalScrollRange + verticalOffset,
                binding.container.paddingRight,
                binding.container.paddingBottom

            )
        }

//    protected abstract fun

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.empty.text = resources.getText(R.string.loading)

        initRecyclerView()
        initAppBar()
    }

    protected lateinit var adapter: A
    protected lateinit var layoutManager: LM

    protected abstract fun initLayoutManager(): LM
    protected abstract fun initAdapter(): A

    protected var isRecyclerViewPrepared: Boolean = false

    private lateinit var adapterDataObserver: RecyclerView.AdapterDataObserver

    private fun initRecyclerView() {

        layoutManager = initLayoutManager()
        adapter = initAdapter()

        adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkEmpty()
                updateHeaderText()
            }
        }
        adapter.registerAdapterDataObserver(adapterDataObserver)

        binding.recyclerView.setUpFastScrollRecyclerViewColor(
            hostFragment.mainActivity,
            ThemeColor.accentColor(App.instance.applicationContext)
        )
        binding.recyclerView.also {
            it.adapter = adapter
            it.layoutManager = layoutManager
        }
        isRecyclerViewPrepared = true
    }

    private fun initAppBar() {

        binding.banner.setExpanded(false)
        binding.banner.addOnOffsetChangedListener(innerAppbarOffsetListener)
        binding.bannerButton.setImageDrawable(
            hostFragment.mainActivity.getTintedDrawable(
                R.drawable.ic_sort_variant_white_24dp,
                binding.bannerText.currentTextColor,
            )
        )
        binding.bannerButton.setOnClickListener {
            hostFragment.popup.onShow = ::configPopup
            hostFragment.popup.onDismiss = ::dismissPopup
            hostFragment.popup.showAtLocation(
                binding.root, Gravity.TOP or Gravity.END, 0,
//                (hostFragment.mainActivity.findViewById<StatusBarView>(R.id.status_bar)?.height ?: 8)
                        8 + hostFragment.totalHeaderHeight + binding.banner.height
            )
        }

        hostFragment.addOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }

    private fun configPopup(popup: ListOptionsPopup) {
        val displayUtil = DisplayUtil(this)

        // grid size
        if (Util.isLandscape(resources)) popup.viewBinding.titleGridSize.text = resources.getText(R.string.action_grid_size_land)
        popup.maxGridSize = displayUtil.maxGridSize
        popup.gridSize = displayUtil.gridSize

        // color footer
        if (this !is GenrePage) { // Genre Page never is colored
            popup.colorFooterVisibility = true
            popup.colorFooterEnability = displayUtil.gridSize > displayUtil.maxGridSizeForList
            popup.colorFooter = displayUtil.colorFooter
        }

        // sort order
        setupSortOrderImpl(displayUtil, popup)
    }

    protected abstract fun setupSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: ListOptionsPopup
    )

    protected fun dismissPopup(popup: ListOptionsPopup) {

        val displayUtil = DisplayUtil(this)

        //  Grid Size
        val gridSizeSelected = popup.gridSize

        if (gridSizeSelected > 0 && gridSizeSelected != displayUtil.gridSize) {

            displayUtil.gridSize = gridSizeSelected
            val itemLayoutRes =
                if (gridSizeSelected > displayUtil.maxGridSizeForList) R.layout.item_grid else R.layout.item_list

            if (adapter.layoutRes != itemLayoutRes) {
                loadDataSet()
                initRecyclerView() // again
            }
            layoutManager.spanCount = gridSizeSelected
        }

        if (this !is GenrePage) {
            // color footer
            val coloredFootersSelected = popup.colorFooter
            if (displayUtil.colorFooter != coloredFootersSelected) {
                displayUtil.colorFooter = coloredFootersSelected
                adapter.usePalette = coloredFootersSelected
                refreshDataSet()
            }
        }

        // sort order
        saveSortOrderImpl(displayUtil, popup)
    }

    protected abstract fun saveSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: ListOptionsPopup,
    )

    protected open val emptyMessage: Int @StringRes get() = R.string.empty
    protected fun checkEmpty() {
        if (isRecyclerViewPrepared) {
            binding.empty.setText(emptyMessage)
            binding.empty.visibility = if (getDataSet().isEmpty()) View.VISIBLE else View.GONE
        }
    }

    protected fun updateHeaderText() {
        binding.bannerText.text = getHeaderText()
    }

    protected abstract fun getHeaderText(): CharSequence

    override fun onMediaStoreChanged() {
        loadDataSet()
        super.onMediaStoreChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.unregisterAdapterDataObserver(adapterDataObserver)

        binding.banner.addOnOffsetChangedListener(innerAppbarOffsetListener)
        hostFragment.removeOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
        _viewBinding = null
    }

    protected val loaderCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onResume() {
        super.onResume()
        if (BuildConfig.DEBUG) Log.v("Metrics", "${System.currentTimeMillis().mod(10000000)} AbsDisplayPage.onResume()")
    }
}
