/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.main.pages

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.github.chr56.android.menu_model.MenuContext
import com.google.android.material.appbar.AppBarLayout
import player.phonograph.R
import player.phonograph.databinding.FragmentDisplayPageBinding
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.model.ItemLayoutStyle
import player.phonograph.model.sort.SortMode
import player.phonograph.ui.modules.popup.ListOptionsPopup
import player.phonograph.util.debug
import player.phonograph.util.logMetrics
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.ui.isLandscape
import util.theme.color.primaryTextColor
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu.NONE
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.coroutines.launch

sealed class AbsPanelPage : AbsPage() {

    private var _viewBinding: FragmentDisplayPageBinding? = null
    protected val binding get() = _viewBinding!!

    //region Lifecycles
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareMediaStoreListener()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        onPrefetchContentDataset()
        _viewBinding = FragmentDisplayPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.empty.text = resources.getText(R.string.loading)

        prepareContentView()

        prepareAppbars()

    }

    override fun onResume() {
        super.onResume()
        debug { logMetrics("BasicDisplayPage.onResume()") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cleanupAppbars()
        _viewBinding = null
    }
    //endregion

    //region Content
    protected abstract fun onPrefetchContentDataset()
    protected abstract fun onContentChanged()
    protected abstract fun prepareContentView()
    protected abstract fun updateContentSetting(
        sortMode: SortMode,
        layout: ItemLayoutStyle,
        gridSize: Int,
        coloredFooter: Boolean,
        shouldRecreate: Boolean,
        shouldReload: Boolean,
    )

    protected fun updateHeaderText(text: CharSequence?) {
        binding.panelText.text = text
    }
    //endregion

    //region PlayerBar and Header
    protected var outerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.container.paddingTop,
                binding.container.paddingRight,
                mainFragment.totalAppBarScrollingRange + verticalOffset
            )
        }

    protected var innerAppbarOffsetListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.container.setPadding(
                binding.container.paddingLeft,
                binding.panel.totalScrollRange + verticalOffset,
                binding.container.paddingRight,
                binding.container.paddingBottom

            )
        }

    private fun prepareAppbars() {
        val context = requireContext()
        binding.panel.setExpanded(false)
        binding.panel.addOnOffsetChangedListener(innerAppbarOffsetListener)

        context.attach(binding.panelToolbar.menu) {
            menuItem(NONE, NONE, 999, getString(R.string.action_settings)) {
                icon = context.getTintedDrawable(
                    R.drawable.ic_sort_variant_white_24dp,
                    context.primaryTextColor(context.nightMode),
                )
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick {
                    val optionsPopup = mainFragment.popup
                    optionsPopup.onShow = ::preparePopup
                    optionsPopup.onDismiss = ::dismissPopup
                    optionsPopup.showAtLocation(
                        binding.root, Gravity.TOP or Gravity.END, 0,
                        8 + mainFragment.totalHeaderHeight + binding.panel.height
                    )
                    true
                }
            }
            prepareAppBarActionButton(this)
        }

        binding.panelText.setTextColor(context.primaryTextColor(context.nightMode))
        binding.panelToolbar.setTitleTextColor(requireContext().primaryTextColor(requireContext().nightMode))

        mainFragment.addOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }

    protected abstract fun prepareAppBarActionButton(menuContext: MenuContext)

    protected inline fun standardAppBarActionButton(
        menuContext: MenuContext,
        crossinline onPlay: (MenuItem) -> Boolean,
        crossinline onShuffle: (MenuItem) -> Boolean,
        clearMenu: Boolean = false,
    ) {
        with(menuContext) {
            if (clearMenu) rootMenu.clear()
            menuItem(getString(R.string.action_play)) {
                icon = context
                    .getTintedDrawable(
                        R.drawable.ic_play_arrow_white_24dp,
                        context.primaryTextColor(context.nightMode)
                    )
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick { onPlay(it) }
            }
            menuItem(getString(R.string.action_shuffle_all)) {
                icon = context
                    .getTintedDrawable(
                        R.drawable.ic_shuffle_white_24dp,
                        context.primaryTextColor(context.nightMode)
                    )
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick { onShuffle(it) }
            }
        }
    }

    private fun cleanupAppbars() {
        binding.panel.removeOnOffsetChangedListener(innerAppbarOffsetListener)
        mainFragment.removeOnAppBarOffsetChangedListener(outerAppbarOffsetListener)
    }

    //endregion

    //region Popup
    protected abstract val displayConfig: PageDisplayConfig

    protected open fun preparePopup(popup: ListOptionsPopup) {
        with(popup) {
            viewBinding.titleGridSize.text =
                if (isLandscape(resources)) {
                    resources.getText(R.string.action_grid_size_land)
                } else {
                    resources.getText(R.string.action_grid_size)
                }
            maxGridSize = displayConfig.maxGridSize
            gridSize = displayConfig.gridSize

            colorFooterVisibility = displayConfig.allowColoredFooter
            if (displayConfig.allowColoredFooter) {
                colorFooterEnability = displayConfig.layout == ItemLayoutStyle.GRID // available in grid mode
                colorFooter = displayConfig.colorFooter
            }

            if (displayConfig.availableSortRefs.isNotEmpty()) {

                val currentSortMode = displayConfig.sortMode

                sortRef = currentSortMode.sortRef
                sortRefAvailable = displayConfig.availableSortRefs

                allowRevert = displayConfig.allowRevertSort
                revert = currentSortMode.revert
            }

            if (displayConfig.availableLayouts.isNotEmpty()) {

                val currentLayout = displayConfig.layout

                itemLayout = currentLayout
                itemLayoutAvailable = displayConfig.availableLayouts
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    protected fun dismissPopup(popup: ListOptionsPopup) {
        val currentDisplayConfig = displayConfig
        var shouldUpdate = false
        var shouldRecreate = false
        var shouldReload = false

        // sort order
        val sortModeSelected = SortMode(popup.sortRef, popup.revert)
        if (sortModeSelected != currentDisplayConfig.sortMode) {
            currentDisplayConfig.sortMode = sortModeSelected
            shouldUpdate = true
            shouldReload = true
        }

        // layout
        val layoutSelected = popup.itemLayout
        if (layoutSelected != currentDisplayConfig.layout) {
            currentDisplayConfig.layout = layoutSelected
            shouldUpdate = true
            shouldRecreate = true
        }

        // grid size
        val gridSizeSelected = popup.gridSize
        if (gridSizeSelected > 0 && gridSizeSelected != currentDisplayConfig.gridSize) {
            currentDisplayConfig.gridSize = gridSizeSelected
            shouldUpdate = true
        }

        // color footer
        if (currentDisplayConfig.allowColoredFooter) {
            val coloredFootersSelected = popup.colorFooter
            if (coloredFootersSelected != currentDisplayConfig.colorFooter) {
                currentDisplayConfig.colorFooter = coloredFootersSelected
                shouldUpdate = true
            }
        }

        if (shouldUpdate) {
            updateContentSetting(
                sortModeSelected,
                layoutSelected,
                gridSizeSelected,
                popup.colorFooter,
                shouldRecreate,
                shouldReload,
            )
        }

        validConfig(currentDisplayConfig)
    }

    private fun validConfig(displayConfig: PageDisplayConfig) {
        var warningLayout: Boolean =
            when (displayConfig.layout) {
                ItemLayoutStyle.GRID    -> displayConfig.gridSize <= 2
                ItemLayoutStyle.LIST_3L -> displayConfig.gridSize > 3
                else                    -> displayConfig.gridSize > 2
            }
        if (warningLayout) {
            Toast.makeText(requireContext(), R.string.warning_inappropriate_config, Toast.LENGTH_SHORT).show()
        }
    }
    //endregion


    //region MediaStoreListener
    private fun prepareMediaStoreListener() {
        lifecycle.addObserver(MediaStoreListener())
    }

    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() {
            lifecycleScope.launch {
                lifecycle.withResumed {
                    onContentChanged()
                }
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            owner.lifecycle.removeObserver(this)
        }
    }
    //endregion

}