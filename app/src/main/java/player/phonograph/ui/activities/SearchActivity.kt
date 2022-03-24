package player.phonograph.ui.activities

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import player.phonograph.R
import player.phonograph.adapter.SearchAdapter
import player.phonograph.databinding.ActivitySearchBinding
import player.phonograph.interfaces.LoaderIds
import player.phonograph.loader.AlbumLoader
import player.phonograph.loader.ArtistLoader
import player.phonograph.loader.SongLoader
import player.phonograph.misc.WrappedAsyncTaskLoader
import player.phonograph.ui.activities.base.AbsMusicServiceActivity
import player.phonograph.util.Util
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer

class SearchActivity :
    AbsMusicServiceActivity(),
    SearchView.OnQueryTextListener,
    LoaderManager.LoaderCallbacks<List<Any>> {

    private var viewBinding: ActivitySearchBinding? = null
    val binding get() = viewBinding!!

    private var searchView: SearchView? = null

    private lateinit var adapter: SearchAdapter
    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setDrawUnderStatusbar()

        Themer.setActivityToolbarColorAuto(this, binding.toolbar)

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SearchAdapter(this, emptyList())
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                binding.empty.visibility = if (adapter.itemCount < 1) View.VISIBLE else View.GONE
            }
        })
        binding.recyclerView.adapter = adapter

        // noinspection ClickableViewAccessibility
        binding.recyclerView.setOnTouchListener { _, _ ->
            hideSoftKeyboard()
            false
        }

        setUpToolBar()

        savedInstanceState?.let { query = it.getString(QUERY) }

        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(QUERY, query)
    }

    private fun setUpToolBar() {
        binding.toolbar.setBackgroundColor(ThemeColor.primaryColor(this))
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        val searchItem = menu!!.findItem(R.id.search)

        searchView = searchItem.actionView as SearchView
        searchView!!.queryHint = getString(R.string.search_hint)
        searchView!!.maxWidth = Int.MAX_VALUE

        searchItem.expandActionView()
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                onBackPressed()
                return false
            }
        })

        searchView!!.setQuery(query, false)
        searchView!!.post { searchView!!.setOnQueryTextListener(this) }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun search(query: String) {
        this.query = query
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this)
    }

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        hideSoftKeyboard()
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        search(newText)
        return false
    }

    private fun hideSoftKeyboard() {
        Util.hideSoftKeyboard(this)
        searchView?.clearFocus()
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Any>> {
        return AsyncSearchResultLoader(this, query)
    }

    override fun onLoadFinished(loader: Loader<List<Any>>, data: List<Any>) {
        adapter.swapDataSet(data)
    }

    override fun onLoaderReset(loader: Loader<List<Any>>) {
        adapter.swapDataSet(emptyList())
    }

    private class AsyncSearchResultLoader(context: Context, private val query: String?) :
        WrappedAsyncTaskLoader<List<Any>>(context) {

        override fun loadInBackground(): List<Any> {
            val results: MutableList<Any> = ArrayList()

            if (!TextUtils.isEmpty(query)) {
                val songs = SongLoader.getSongs(context, query!!.trim { it <= ' ' })
                if (songs.isNotEmpty()) {
                    results.add(context.resources.getString(R.string.songs))
                    results.addAll(songs)
                }
                val artists = ArtistLoader.getArtists(context, query.trim { it <= ' ' })
                if (artists.isNotEmpty()) {
                    results.add(context.resources.getString(R.string.artists))
                    results.addAll(artists)
                }
                val albums = AlbumLoader.getAlbums(context, query.trim { it <= ' ' })
                if (albums.isNotEmpty()) {
                    results.add(context.resources.getString(R.string.albums))
                    results.addAll(albums)
                }
            }

            return results
        }
    }

    companion object {
        const val QUERY = "query"
        private const val LOADER_ID = LoaderIds.SEARCH_ACTIVITY
    }
}
