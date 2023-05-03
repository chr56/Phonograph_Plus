/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.pages

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.google.android.material.appbar.AppBarLayout
import mt.pref.ThemeColor
import mt.util.color.primaryTextColor
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.Displayable
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.fragments.pages.util.DisplayConfig
import player.phonograph.ui.fragments.pages.util.DisplayConfigTarget
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.ui.isLandscape
import player.phonograph.util.ui.setUpFastScrollRecyclerViewColor
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu.NONE
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


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
                binding.panel.totalScrollRange + verticalOffset,
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

    internal abstract val displayConfigTarget: DisplayConfigTarget

    private fun initAppBar() {

        binding.panel.setExpanded(false)
        binding.panel.addOnOffsetChangedListener(innerAppbarOffsetListener)

        val context = hostFragment.mainActivity
        context.attach(binding.panelToolbar.menu) {
            menuItem(NONE, NONE, 999, getString(R.string.action_settings)) {
                icon = context.getTintedDrawable(
                    R.drawable.ic_sort_variant_white_24dp,
                    context.primaryTextColor(context.nightMode),
                )
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    hostFragment.popup.onShow = ::configPopup
                    hostFragment.popup.onDismiss = ::dismissPopup
                    hostFragment.popup.showAtLocation(
                        binding.root, Gravity.TOP or Gravity.END, 0,
                        8 + hostFragment.totalHeaderHeight + binding.panel.height
                    )
                    true
                }
            }
        }
        binding.panelToolbar.setTitleTextColor(requireContext().primaryTextColor(requireContext().nightMode))

        configAppBar(binding.panelToolbar)

        hostFragment.addOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }

    protected open fun configAppBar(panelToolbar: Toolbar) {}

    private fun configPopup(popup: ListOptionsPopup) {
        val displayConfig = DisplayConfig(displayConfigTarget)

        // grid size
        if (isLandscape(resources)) popup.viewBinding.titleGridSize.text =
            resources.getText(R.string.action_grid_size_land)
        popup.maxGridSize = displayConfig.maxGridSize
        popup.gridSize = displayConfig.gridSize

        // color footer
        if (this !is GenrePage) { // Genre Page never is colored
            popup.colorFooterVisibility = true
            popup.colorFooterEnability = displayConfig.gridSize > displayConfig.maxGridSizeForList
            popup.colorFooter = displayConfig.colorFooter
        }

        // sort order
        setupSortOrderImpl(displayConfig, popup)
    }

    protected abstract fun setupSortOrderImpl(
        displayConfig: DisplayConfig,
        popup: ListOptionsPopup,
    )

    protected fun dismissPopup(popup: ListOptionsPopup) {

        val displayConfig = DisplayConfig(displayConfigTarget)

        //  Grid Size
        val gridSizeSelected = popup.gridSize

        if (gridSizeSelected > 0 && gridSizeSelected != displayConfig.gridSize) {

            displayConfig.gridSize = gridSizeSelected
            val itemLayoutRes =
                if (gridSizeSelected > displayConfig.maxGridSizeForList) R.layout.item_grid else R.layout.item_list

            if (adapter.layoutRes != itemLayoutRes) {
                loadDataSet()
                initRecyclerView() // again
            }
            layoutManager.spanCount = gridSizeSelected
        }

        if (this !is GenrePage) {
            // color footer
            val coloredFootersSelected = popup.colorFooter
            if (displayConfig.colorFooter != coloredFootersSelected) {
                displayConfig.colorFooter = coloredFootersSelected
                adapter.usePalette = coloredFootersSelected
                refreshDataSet()
            }
        }

        // sort order
        saveSortOrderImpl(displayConfig, popup)
    }

    protected abstract fun saveSortOrderImpl(
        displayConfig: DisplayConfig,
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
        binding.panelText.text = getHeaderText()
    }

    protected abstract fun getHeaderText(): CharSequence

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.unregisterAdapterDataObserver(adapterDataObserver)

        binding.panel.removeOnOffsetChangedListener(innerAppbarOffsetListener)
        hostFragment.removeOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
        _viewBinding = null
    }

    private lateinit var listener: MediaStoreListener
    override fun onCreate(savedInstanceState: Bundle?) {
        listener = MediaStoreListener()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(listener)
    }


    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() {
            loadDataSet()
        }
    }


    protected val loaderCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onResume() {
        super.onResume()
        if (BuildConfig.DEBUG) Log.v("Metrics", "${System.currentTimeMillis().mod(10000000)} AbsDisplayPage.onResume()")
    }
}
