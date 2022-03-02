package player.phonograph.ui.fragments.mainactivity.library.pager;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;

import player.phonograph.R;
import player.phonograph.adapter.artist.ArtistAdapter;
import player.phonograph.interfaces.LoaderIds;
import player.phonograph.loader.ArtistLoader;
import player.phonograph.misc.WrappedAsyncTaskLoader;
import player.phonograph.model.Artist;
import player.phonograph.settings.PreferenceUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class ArtistsFragment extends AbsLibraryPagerRecyclerViewCustomGridSizeFragment<ArtistAdapter, GridLayoutManager> implements LoaderManager.LoaderCallbacks<List<Artist>> {

    private static final int LOADER_ID = LoaderIds.ARTISTS_FRAGMENT;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @NonNull
    @Override
    protected GridLayoutManager createLayoutManager() {
        return new GridLayoutManager(requireActivity(), getGridSize());
    }

    @NonNull
    @Override
    protected ArtistAdapter createAdapter() {
        int itemLayoutRes = getItemLayoutRes();
        notifyLayoutResChanged(itemLayoutRes);
        List<Artist> dataSet = getAdapter() == null ? new ArrayList<>() : getAdapter().getDataSet();
        return new ArtistAdapter(
                getLibraryFragment().getMainActivity(),
                dataSet,
                itemLayoutRes,
                loadUsePalette(),
                getLibraryFragment());
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.no_artists;
    }

    @Override
    public void onMediaStoreChanged() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    protected String loadSortOrder() {
        return PreferenceUtil.getInstance(requireActivity()).getArtistSortOrder();
    }

    @Override
    protected void saveSortOrder(String sortOrder) {
        PreferenceUtil.getInstance(requireActivity()).setArtistSortOrder(sortOrder);
    }

    @Override
    protected void setSortOrder(String sortOrder) {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    protected int loadGridSize() {
        return PreferenceUtil.getInstance(requireActivity()).getArtistGridSize();
    }

    @Override
    protected void saveGridSize(int gridSize) {
        PreferenceUtil.getInstance(requireActivity()).setArtistGridSize(gridSize);
    }

    @Override
    protected int loadGridSizeLand() {
        return PreferenceUtil.getInstance(requireActivity()).getArtistGridSizeLand();
    }

    @Override
    protected void saveGridSizeLand(int gridSize) {
        PreferenceUtil.getInstance(requireActivity()).setArtistGridSizeLand(gridSize);
    }

    @Override
    protected void saveUsePalette(boolean usePalette) {
        PreferenceUtil.getInstance(requireActivity()).setArtistColoredFooters(usePalette);
    }

    @Override
    public boolean loadUsePalette() {
        return PreferenceUtil.getInstance(requireActivity()).artistColoredFooters();
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


    @NonNull
    @Override
    public Loader<List<Artist>> onCreateLoader(int id, Bundle args) {
        return new AsyncArtistLoader(requireActivity());
    }


    @Override
    public void onLoadFinished(@NonNull Loader<List<Artist>> loader, List<Artist> data) {
        getAdapter().swapDataSet(data);
    }


    @Override
    public void onLoaderReset(@NonNull Loader<List<Artist>> loader) {
        getAdapter().swapDataSet(new ArrayList<>());
    }

    private static class AsyncArtistLoader extends WrappedAsyncTaskLoader<List<Artist>> {
        public AsyncArtistLoader(Context context) {
            super(context);
        }

        @Override
        public List<Artist> loadInBackground() {
            return ArtistLoader.getAllArtists(getContext());
        }
    }
}
