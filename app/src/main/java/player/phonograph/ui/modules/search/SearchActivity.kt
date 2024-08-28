/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.search

import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.phonograph.misc.menuProvider
import lib.storage.launcher.CreateFileStorageAccessDelegate
import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.R
import player.phonograph.databinding.ActivitySearchBinding
import player.phonograph.databinding.PopupWindowSearchBinding
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.base.AbsMusicServiceActivity
import player.phonograph.ui.components.popup.OptionsPopup
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.primaryColor
import player.phonograph.util.theme.accentColor
import player.phonograph.util.ui.hideKeyboard
import util.theme.color.primaryTextColor
import util.theme.color.secondaryTextColor
import util.theme.view.searchview.setSearchViewContentColor
import util.theme.view.toolbar.setToolbarColor
import util.theme.view.toolbar.tintCollapseIcon
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.launch

class SearchActivity : AbsMusicServiceActivity(), SearchView.OnQueryTextListener,
                       ICreateFileStorageAccessible, IOpenFileStorageAccessible {

    private var viewBinding: ActivitySearchBinding? = null
    val binding get() = viewBinding!!

    private val viewModel: SearchActivityViewModel by viewModels()

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()

    private lateinit var searchResultPageAdapter: SearchResultPageAdapter
    private lateinit var mediator: TabLayoutMediator

    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        viewBinding = ActivitySearchBinding.inflate(layoutInflater)

        registerActivityResultLauncherDelegate(
            createFileStorageAccessDelegate,
            openFileStorageAccessDelegate,
        )

        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        setUpToolBar()
        setUpPager()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.query.collect { text ->
                    searchView?.setQuery(text, false)
                }
            }
        }
        lifecycleScope.launch {
            val disableRealTimeSearchFlow = Setting(this@SearchActivity)[Keys.disableRealTimeSearch].flow
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                disableRealTimeSearchFlow.collect {
                    disableRealTimeSearch = it
                }
            }
        }

        lifecycle.addObserver(MediaStoreListener())
    }

    private fun setUpPager() {
        val primaryColor = primaryColor()
        val accentColor = accentColor()
        searchResultPageAdapter = SearchResultPageAdapter(this)
        with(binding) {
            with(pager) {
                adapter = searchResultPageAdapter
                orientation = ViewPager2.ORIENTATION_HORIZONTAL
                offscreenPageLimit = 1
                setOnClickListener {
                    hideSoftKeyboard()
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
            tab.text = getText(SearchResultPageAdapter.TabType.values()[i].nameRes)
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
    }

    private fun setupMenu(menu: Menu) {
        menuInflater.inflate(R.menu.menu_search, menu)

        val searchItem = menu.findItem(R.id.search)

        searchView = searchItem.actionView as SearchView
        searchView!!.queryHint = getString(R.string.search_hint)
        searchView!!.maxWidth = Int.MAX_VALUE

        searchItem.expandActionView()
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                onBackPressedDispatcher.onBackPressed()
                return false
            }
        })

        searchView!!.post { searchView!!.setOnQueryTextListener(this) }

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

    private inner class MediaStoreListener : MediaStoreTracker.LifecycleListener() {
        override fun onMediaStoreChanged() {
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
