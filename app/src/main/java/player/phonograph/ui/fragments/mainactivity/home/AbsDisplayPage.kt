/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.mainactivity.home

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.databinding.PopupWindowMainBinding
import player.phonograph.interfaces.Displayable
import player.phonograph.util.Util
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor
import player.phonograph.views.StatusBarView
import util.mdcolor.pref.ThemeColor

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
                binding.innerAppBar.totalScrollRange + verticalOffset,
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

        binding.innerAppBar.setExpanded(false)
        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)
        val actionDrawable = AppCompatResources.getDrawable(
            hostFragment.mainActivity,
            R.drawable.ic_sort_variant_white_24dp
        )
        actionDrawable?.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(
                binding.textPageHeader.currentTextColor,
                BlendModeCompat.SRC_IN
            )
        binding.buttonPageHeader.setImageDrawable(actionDrawable)
        binding.buttonPageHeader.setOnClickListener {
            hostFragment.popup.onShow = ::configPopup
            hostFragment.popup.onDismiss = ::dismissPopup
            hostFragment.popup.showAtLocation(
                binding.root, Gravity.TOP or Gravity.END, 0,
                (hostFragment.mainActivity.findViewById<StatusBarView>(R.id.status_bar)?.height ?: 8) + hostFragment.totalHeaderHeight + binding.innerAppBar.height
            )
        }

        hostFragment.addOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }

    private fun configPopup(popup: PopupWindowMainBinding) {
        val displayUtil = DisplayUtil(this)

        // grid size
        popup.titleGridSize.visibility = View.VISIBLE
        popup.groupGridSize.visibility = View.VISIBLE
        if (Util.isLandscape(resources)) popup.titleGridSize.text = resources.getText(R.string.action_grid_size_land)
        val current = displayUtil.gridSize
        val max = displayUtil.maxGridSize
        for (i in 0 until max) popup.groupGridSize.getChildAt(i).visibility = View.VISIBLE
        popup.groupGridSize.clearCheck()
        (popup.groupGridSize.getChildAt(current - 1) as RadioButton).isChecked = true

        // color footer
        if (this !is GenrePage) { // Genre Page never colored
            popup.actionColoredFooters.visibility = View.VISIBLE
            popup.actionColoredFooters.isChecked = displayUtil.colorFooter
            popup.actionColoredFooters.isEnabled = displayUtil.gridSize > displayUtil.maxGridSizeForList
        }

        // sort order

        // clear existed
        popup.groupSortOrderMethod.visibility = View.VISIBLE
        popup.titleSortOrderMethod.visibility = View.VISIBLE
        popup.groupSortOrderRef.visibility = View.VISIBLE
        popup.titleSortOrderRef.visibility = View.VISIBLE
        for (i in 0 until popup.groupSortOrderRef.childCount) popup.groupSortOrderRef.getChildAt(i).visibility = View.GONE

        setupSortOrderImpl(displayUtil, popup)
    }

    protected abstract fun setupSortOrderImpl(
        displayUtil: DisplayUtil,
        popup: PopupWindowMainBinding
    )

    protected fun dismissPopup(popup: PopupWindowMainBinding) {

        val displayUtil = DisplayUtil(this)

        //  Grid Size
        var gridSizeSelected = 0
        for (i in 0 until displayUtil.maxGridSize) {
            if ((popup.groupGridSize.getChildAt(i) as RadioButton).isChecked) {
                gridSizeSelected = i + 1
                break
            }
        }

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
            val coloredFootersSelected = popup.actionColoredFooters.isChecked
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
        popup: PopupWindowMainBinding,
    )

    protected open val emptyMessage: Int @StringRes get() = R.string.empty
    protected fun checkEmpty() {
        if (isRecyclerViewPrepared) {
            binding.empty.setText(emptyMessage)
            binding.empty.visibility = if (getDataSet().isEmpty()) View.VISIBLE else View.GONE
        }
    }

    protected fun updateHeaderText() {
        binding.textPageHeader.text = getHeaderText()
    }

    protected abstract fun getHeaderText(): CharSequence

    override fun onMediaStoreChanged() {
        loadDataSet()
        super.onMediaStoreChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.unregisterAdapterDataObserver(adapterDataObserver)

        binding.innerAppBar.addOnOffsetChangedListener(innerAppbarOffsetListener)
        hostFragment.removeOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
        _viewBinding = null
    }

    protected val loaderCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onResume() {
        super.onResume()
        if (BuildConfig.DEBUG) Log.v("Metrics", "${System.currentTimeMillis().mod(10000000)} AbsDisplayPage.onResume()")
    }
}
