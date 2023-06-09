package player.phonograph.ui.activities

import lib.phonograph.misc.menuProvider
import mt.tint.setActivityToolbarColor
import mt.tint.viewtint.setSearchViewContentColor
import mt.tint.viewtint.tintCollapseIcon
import mt.util.color.primaryTextColor
import player.phonograph.R
import player.phonograph.adapter.SearchAdapter
import player.phonograph.databinding.ActivitySearchBinding
import player.phonograph.mechanism.event.MediaStoreTracker
import player.phonograph.ui.activities.base.AbsMusicServiceActivity
import player.phonograph.util.ui.hideKeyboard
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.coroutines.launch

class SearchActivity : AbsMusicServiceActivity(), SearchView.OnQueryTextListener {

    private var viewBinding: ActivitySearchBinding? = null
    val binding get() = viewBinding!!

    private val viewModel: SearchActivityViewModel by viewModels()

    private lateinit var adapter: SearchAdapter
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        viewBinding = ActivitySearchBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        setUpRecyclerView()
        setUpToolBar()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.query.collect { text ->
                    searchView?.setQuery(text, false)
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.results.collect { results ->
                    binding.empty.visibility = if (results.isEmpty()) View.GONE else View.VISIBLE
                    adapter.dataSet = results
                }
            }
        }

        lifecycle.addObserver(MediaStoreListener())
    }

    private fun setUpRecyclerView() {
        adapter = SearchAdapter(this, emptyList())
        with(binding) {
            recyclerView.layoutManager = LinearLayoutManager(this@SearchActivity)
            recyclerView.adapter = adapter
            // noinspection ClickableViewAccessibility
            recyclerView.setOnTouchListener { _, _ ->
                hideSoftKeyboard()
                false
            }
        }
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                binding.empty.visibility = if (adapter.itemCount < 1) View.VISIBLE else View.GONE
            }
        })
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
