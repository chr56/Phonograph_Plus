/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities.search

import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import lib.phonograph.misc.menuProvider
import mt.tint.setActivityToolbarColor
import mt.tint.viewtint.setSearchViewContentColor
import mt.tint.viewtint.tintCollapseIcon
import mt.util.color.primaryTextColor
import player.phonograph.R
import player.phonograph.databinding.ActivitySearchBinding
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.ui.activities.base.AbsMusicServiceActivity
import player.phonograph.util.ui.hideKeyboard
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.launch

class SearchActivity : AbsMusicServiceActivity(), SearchView.OnQueryTextListener {

    private var viewBinding: ActivitySearchBinding? = null
    val binding get() = viewBinding!!

    private val viewModel: SearchActivityViewModel by viewModels()


    private lateinit var searchResultPageAdapter: SearchResultPageAdapter
    private lateinit var mediator: TabLayoutMediator

    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        viewBinding = ActivitySearchBinding.inflate(layoutInflater)
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

        lifecycle.addObserver(MediaStoreListener())
    }

    private fun setUpPager() {
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
                setBackgroundColor(primaryColor)
            }
        }
        mediator = TabLayoutMediator(binding.tabs, binding.pager) { tab: TabLayout.Tab, i: Int ->
            tab.text = getText(SearchResultPageAdapter.TabType.values()[i].nameRes)
        }
        mediator.attach()
    }

    private fun setUpToolBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        addMenuProvider(menuProvider(this::setupMenu))
        setActivityToolbarColor(binding.toolbar, primaryColor)
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

        val textColor = primaryTextColor(primaryColor)
        binding.toolbar.tintCollapseIcon(textColor)
        setSearchViewContentColor(searchView, textColor)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        hideSoftKeyboard()
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        viewModel.query(this, newText)
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

}
