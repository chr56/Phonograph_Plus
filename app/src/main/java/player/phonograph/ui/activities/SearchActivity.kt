package player.phonograph.ui.activities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import mt.tint.setActivityToolbarColor
import mt.util.color.primaryTextColor
import player.phonograph.R
import player.phonograph.adapter.SearchAdapter
import player.phonograph.databinding.ActivitySearchBinding
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.mediastore.SongLoader
import player.phonograph.ui.activities.base.AbsMusicServiceActivity
import player.phonograph.util.ImageUtil.drawableColorFilter
import player.phonograph.util.ImageUtil.makeContrastDrawable
import player.phonograph.util.ReflectUtil.reflectDeclaredField
import player.phonograph.util.Util
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView

class SearchActivity :
        AbsMusicServiceActivity(),
        SearchView.OnQueryTextListener {

    private var viewBinding: ActivitySearchBinding? = null
    val binding get() = viewBinding!!

    private var searchView: SearchView? = null

    private lateinit var adapter: SearchAdapter
    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SearchAdapter(this, emptyList())
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                binding.empty.visibility = if (adapter.itemCount < 1) View.VISIBLE else View.GONE
            }
        })
        binding.recyclerView.adapter = adapter

        isRecyclerViewPrepared = true

        // noinspection ClickableViewAccessibility
        binding.recyclerView.setOnTouchListener { _, _ ->
            hideSoftKeyboard()
            false
        }

        setUpToolBar()

        savedInstanceState?.let { query = it.getString(QUERY) }
    }

    private var isRecyclerViewPrepared: Boolean = false

    private fun loadDataSet(context: Context, query: String) {
        loaderCoroutineScope.launch {

            val results: MutableList<Any> = ArrayList()

            if (!TextUtils.isEmpty(query)) {
                val songs = SongLoader.getSongs(context, query.trim { it <= ' ' })
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

            while (!isRecyclerViewPrepared) yield() // wait until ready

            withContext(Dispatchers.Main) {
                if (isRecyclerViewPrepared) adapter.dataSet = results
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(QUERY, query)
    }

    private fun setUpToolBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setActivityToolbarColor(binding.toolbar, primaryColor)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
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
                onBackPressed()
                return false
            }
        })

        searchView!!.setQuery(query, false)
        searchView!!.post { searchView!!.setOnQueryTextListener(this) }

        searchView!!.adjustColor(primaryTextColor(primaryColor))
        with(binding.toolbar) {
            collapseIcon = makeContrastDrawable(collapseIcon, primaryTextColor(primaryColor))
        }

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
        loadDataSet(this, query)
    }

    private val loaderCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        if (!query.isNullOrEmpty()) loadDataSet(this, query!!)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        hideSoftKeyboard()
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        search(newText)
        searchView?.adjustColor(primaryTextColor(primaryColor))
        with(binding.toolbar) {
            collapseIcon = makeContrastDrawable(collapseIcon, primaryTextColor(primaryColor))
        }
        return false
    }

    private fun hideSoftKeyboard() {
        Util.hideSoftKeyboard(this)
        searchView?.clearFocus()
    }



    companion object {
        const val QUERY = "query"

        private fun SearchView.adjustColor(color: Int) {
            try {
                setQueryTextColor(this, color)
                setIconDrawableColor(this, color)
            } catch (e: Exception) {
                Log.w("SearchViewReflect", e)
            }
        }

        @Throws(NoSuchFieldException::class, SecurityException::class)
        private fun setQueryTextColor(searchView: SearchView, color: Int) {
            val textView: TextView = searchView.reflectDeclaredField("mSearchSrcTextView")
            textView.setTextColor(color)
        }

        @Throws(NoSuchFieldException::class, SecurityException::class)
        private fun setIconDrawableColor(searchView: SearchView, color: Int) {
            val closeButton: ImageView = searchView.reflectDeclaredField("mCloseButton")
            closeButton.colorFilter = drawableColorFilter(color, BlendModeCompat.SRC_IN)
            // val searchButton: ImageView = searchView.reflectDeclaredField("mSearchButton")
            // searchButton.colorFilter = drawableColorFilter(color, BlendModeCompat.SRC_OUT)
        }
    }
}
