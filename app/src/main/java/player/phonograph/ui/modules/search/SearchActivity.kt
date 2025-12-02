/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.search

import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.launcher.CreateFileStorageAccessDelegate
import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenDirStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDirStorageAccessDelegate
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.R
import player.phonograph.databinding.ActivitySearchBinding
import player.phonograph.databinding.PopupWindowSearchBinding
import player.phonograph.mechanism.event.EventHub
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.ui.modules.popup.OptionsPopup
import player.phonograph.util.observe
import player.phonograph.util.theme.ThemeSettingsDelegate.accentColor
import player.phonograph.util.theme.ThemeSettingsDelegate.primaryColor
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.updateSystemBarsColor
import player.phonograph.util.ui.hideKeyboard
import player.phonograph.util.ui.menuProvider
import util.theme.color.darkenColor
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import util.theme.view.searchview.setSearchViewContentColor
import util.theme.view.toolbar.setToolbarColor
import util.theme.view.toolbar.tintCollapseIcon
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.viewpager2.widget.ViewPager2
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo

class SearchActivity : AbsSlidingMusicPanelActivity(), SearchView.OnQueryTextListener,
                       ICreateFileStorageAccessible, IOpenFileStorageAccessible, IOpenDirStorageAccessible {

    private var viewBinding: ActivitySearchBinding? = null
    val binding get() = viewBinding!!

    private val viewModel: SearchActivityViewModel by viewModels()

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()

    private lateinit var searchResultPageAdapter: SearchResultPageAdapter
    private lateinit var mediator: TabLayoutMediator

    private var searchView: SearchView? = null
    private var isKeyboardVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        viewBinding = ActivitySearchBinding.inflate(layoutInflater)

        registerActivityResultLauncherDelegate(
            createFileStorageAccessDelegate,
            openFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
        )

        super.onCreate(savedInstanceState)

        setUpToolBar()
        setUpPager()

        updateSystemBarsColor(darkenColor(primaryColor()), Color.TRANSPARENT)

        observe(viewModel.query) { text -> searchView?.setQuery(text, false) }
        observe(Setting(this@SearchActivity)[Keys.disableRealTimeSearch].flow) { disableRealTimeSearch = it }
        lifecycle.addObserver(MediaStoreListener())
    }

    override fun createContentView(): View = wrapSlidingMusicPanel(binding.root)

    private fun setUpPager() {
        val primaryColor = primaryColor()
        val accentColor = accentColor()
        searchResultPageAdapter = SearchResultPageAdapter(this)
        with(binding) {
            with(pager) {
                adapter = searchResultPageAdapter
                orientation = ViewPager2.ORIENTATION_HORIZONTAL
                offscreenPageLimit = 1
                for (view in children) {
                    view.setOnTouchListener { view, _ ->
                        if (isKeyboardVisible) {
                            hideSoftKeyboard()
                            true
                        } else {
                            view.performClick()
                        }
                    }
                }
            }
            with(tabs) {
                tabMode = TabLayout.MODE_SCROLLABLE
                setTabTextColors(secondaryTextColor(primaryColor), primaryTextColor(primaryColor))
                setSelectedTabIndicatorColor(accentColor)
            }
            with(actionBarContainer) {
                setBackgroundColor(primaryColor)
            }
        }
        mediator = TabLayoutMediator(binding.tabs, binding.pager) { tab: TabLayout.Tab, i: Int ->
            tab.text = getText(SearchResultPageAdapter.TabType.entries[i].nameRes)
        }
        mediator.attach()
        with(binding.config) {
            setImageDrawable(getTintedDrawable(R.drawable.ic_settings_white_24dp, primaryTextColor(primaryColor)))
            setBackgroundDrawable(null)
            setOnClickListener {
                if (popup == null) {
                    popup = SearchOptionsPopup(this@SearchActivity)
                }
                popup?.showAsDropDown(this)
            }
        }
    }


    private fun setUpToolBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setToolbarColor(binding.toolbar, primaryColor())

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            isKeyboardVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
            windowInsets
        }
    }

    private fun setupMenu(menu: Menu) {
        menuInflater.inflate(R.menu.menu_search, menu)

        val searchItem = menu.findItem(R.id.search).apply {
            expandActionView()
            setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    if (isKeyboardVisible) {
                        hideSoftKeyboard()
                    } else {
                        onBackPressedDispatcher.onBackPressed()
                    }
                    return false
                }
            })
        }


        searchView = (searchItem.actionView as SearchView).apply {
            queryHint = getString(R.string.tips_search_hint)
            maxWidth = Int.MAX_VALUE
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            post {
                setOnQueryTextListener(this@SearchActivity)
            }
        }

        val textColor = primaryTextColor(primaryColor())

        binding.toolbar.tintCollapseIcon(textColor)
        setSearchViewContentColor(searchView, textColor)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        viewModel.query(this, query)
        hideSoftKeyboard()
        return false
    }

    private var disableRealTimeSearch: Boolean = false
    override fun onQueryTextChange(newText: String): Boolean {
        if (!disableRealTimeSearch) {
            viewModel.query(this, newText)
        }
        return false
    }

    private fun hideSoftKeyboard() {
        hideKeyboard(this)
        searchView?.clearFocus()
    }

    private inner class MediaStoreListener :
            EventHub.LifeCycleEventReceiver(this, EventHub.EVENT_MUSIC_LIBRARY_CHANGED) {
        override fun onEventReceived(context: Context, intent: Intent) {
            viewModel.refresh(this@SearchActivity)
        }
    }

    private var popup: SearchOptionsPopup? = null

    inner class SearchOptionsPopup private constructor(
        private val popupBinding: PopupWindowSearchBinding,
    ) : OptionsPopup(popupBinding) {

        constructor(context: Context) : this(PopupWindowSearchBinding.inflate(LayoutInflater.from(context)))

        override fun onShow() {
            super.onShow()
            prepareColors(contentView.context)
            popupBinding.checkboxDisableRealTimeSearch.isChecked = disableRealTimeSearch
            popupBinding.checkboxDisableRealTimeSearch.buttonTintList = widgetColor
        }

        override fun dismiss() {
            super.dismiss()
            disableRealTimeSearch = popupBinding.checkboxDisableRealTimeSearch.isChecked
        }

    }
}
