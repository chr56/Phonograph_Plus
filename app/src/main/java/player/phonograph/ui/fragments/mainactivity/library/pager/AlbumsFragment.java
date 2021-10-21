package player.phonograph.ui.fragments.mainactivity.library.pager;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;

import player.phonograph.R;
import player.phonograph.adapter.album.AlbumAdapter;
import player.phonograph.interfaces.LoaderIds;
import player.phonograph.loader.AlbumLoader;
import player.phonograph.misc.WrappedAsyncTaskLoader;
import player.phonograph.model.Album;
import player.phonograph.util.PreferenceUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class AlbumsFragment extends AbsLibraryPagerRecyclerViewCustomGridSizeFragment<AlbumAdapter, GridLayoutManager> implements LoaderManager.LoaderCallbacks<List<Album>> {

    private static final int LOADER_ID = LoaderIds.ALBUMS_FRAGMENT;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    protected GridLayoutManager createLayoutManager() {
        return new GridLayoutManager(requireActivity(), getGridSize());
    }

    @NonNull
    @Override
    protected AlbumAdapter createAdapter() {
        int itemLayoutRes = getItemLayoutRes();
        notifyLayoutResChanged(itemLayoutRes);
        List<Album> dataSet = getAdapter() == null ? new ArrayList<>() : getAdapter().getDataSet();
        return new AlbumAdapter(
                getLibraryFragment().getMainActivity(),
                dataSet,
                itemLayoutRes,
                loadUsePalette(),
                getLibraryFragment());
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.no_albums;
    }

    @Override
    protected String loadSortOrder() {
        return PreferenceUtil.getInstance(requireActivity()).getAlbumSortOrder();
    }

    @Override
    protected void saveSortOrder(String sortOrder) {
        PreferenceUtil.getInstance(requireActivity()).setAlbumSortOrder(sortOrder);
    }

    @Override
    protected void setSortOrder(String sortOrder) {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public boolean loadUsePalette() {
        return PreferenceUtil.getInstance(requireActivity()).albumColoredFooters();
    }

    @Override
    protected void setUsePalette(boolean usePalette) {
        getAdapter().usePalette(usePalette);
    }

    @Override
    protected void setGridSize(int gridSize) {
        getLayoutManager().setSpanCount(gridSize);
        getAdapter().notifyDataSetChanged();
    }

    @Override
    protected int loadGridSize() {
        return PreferenceUtil.getInstance(requireActivity()).getAlbumGridSize(requireActivity());
    }

    @Override
    protected void saveGridSize(int gridSize) {
        PreferenceUtil.getInstance(requireActivity()).setAlbumGridSize(gridSize);
    }

    @Override
    protected int loadGridSizeLand() {
        return PreferenceUtil.getInstance(requireActivity()).getAlbumGridSizeLand(requireActivity());
    }

    @Override
    protected void saveGridSizeLand(int gridSize) {
        PreferenceUtil.getInstance(requireActivity()).setAlbumGridSizeLand(gridSize);
    }

    @Override
    protected void saveUsePalette(boolean usePalette) {
        PreferenceUtil.getInstance(requireActivity()).setAlbumColoredFooters(usePalette);
    }

    @Override
    public void onMediaStoreChanged() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @NonNull
    @Override
    public Loader<List<Album>> onCreateLoader(int id, Bundle args) {
        return new AsyncAlbumLoader(requireActivity());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Album>> loader, List<Album> data) {
        getAdapter().swapDataSet(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Album>> loader) {
        getAdapter().swapDataSet(new ArrayList<>());
    }

    private static class AsyncAlbumLoader extends WrappedAsyncTaskLoader<List<Album>> {
        public AsyncAlbumLoader(Context context) {
            super(context);
        }

        @Override
        public List<Album> loadInBackground() {
            return AlbumLoader.getAllAlbums(getContext());
        }
    }
}
