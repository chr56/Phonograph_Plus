package player.phonograph.ui.fragments.mainactivity.library.pager

import android.content.Context
import android.os.Bundle
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import player.phonograph.R
import player.phonograph.adapter.GenreAdapter
import player.phonograph.interfaces.LoaderIds
import player.phonograph.loader.GenreLoader.getAllGenres
import player.phonograph.misc.WrappedAsyncTaskLoader
import player.phonograph.model.Genre
import java.util.ArrayList

// todo null safety
class GenresFragment : AbsLibraryPagerRecyclerViewFragment<GenreAdapter, LinearLayoutManager>(), LoaderManager.LoaderCallbacks<List<Genre>> {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loaderManager.initLoader(LOADER_ID, null, this)
    }

    override fun createLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(activity)
    }

    override fun createAdapter(): GenreAdapter {
        val dataSet = if (adapter == null) ArrayList() else adapter!!.dataSet
        return GenreAdapter(libraryFragment!!.mainActivity, dataSet, R.layout.item_list_no_image)
    }

    override val emptyMessage: Int = R.string.no_genres

    override fun onMediaStoreChanged() {
        loaderManager.restartLoader(LOADER_ID, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Genre>> {
        return AsyncGenreLoader(libraryFragment!!.mainActivity)
    }

    override fun onLoadFinished(loader: Loader<List<Genre>>, data: List<Genre>) {
        adapter!!.swapDataSet(data)
    }

    override fun onLoaderReset(loader: Loader<List<Genre>>) {
        adapter!!.swapDataSet(ArrayList())
    }

    private class AsyncGenreLoader(context: Context) : WrappedAsyncTaskLoader<List<Genre>>(context) {
        override fun loadInBackground(): List<Genre> = getAllGenres(context)
    }

    companion object {
        private const val LOADER_ID = LoaderIds.GENRES_FRAGMENT
    }
}
