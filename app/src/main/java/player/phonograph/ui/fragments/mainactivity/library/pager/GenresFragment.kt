package player.phonograph.ui.fragments.mainactivity.library.pager;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;

import player.phonograph.R;
import player.phonograph.adapter.GenreAdapter;
import player.phonograph.interfaces.LoaderIds;
import player.phonograph.loader.GenreLoader;
import player.phonograph.misc.WrappedAsyncTaskLoader;
import player.phonograph.model.Genre;

import java.util.ArrayList;
import java.util.List;

public class GenresFragment extends AbsLibraryPagerRecyclerViewFragment<GenreAdapter, LinearLayoutManager> implements LoaderManager.LoaderCallbacks<List<Genre>> {

    private static final int LOADER_ID = LoaderIds.GENRES_FRAGMENT;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @NonNull
    @Override
    protected LinearLayoutManager createLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    @NonNull
    @Override
    protected GenreAdapter createAdapter() {
        List<Genre> dataSet = getAdapter() == null ? new ArrayList<>() : getAdapter().getDataSet();
        return new GenreAdapter(getLibraryFragment().getMainActivity(), dataSet, R.layout.item_list_no_image);
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.no_genres;
    }

    @Override
    public void onMediaStoreChanged() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    @NonNull
    public Loader<List<Genre>> onCreateLoader(int id, Bundle args) {
        return new GenresFragment.AsyncGenreLoader(getActivity());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Genre>> loader, List<Genre> data) {
        getAdapter().swapDataSet(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Genre>> loader) {
        getAdapter().swapDataSet(new ArrayList<>());
    }

    private static class AsyncGenreLoader extends WrappedAsyncTaskLoader<List<Genre>> {
        public AsyncGenreLoader(Context context) {
            super(context);
        }

        @Override
        public List<Genre> loadInBackground() {
            return GenreLoader.getAllGenres(getContext());
        }
    }
}
