package player.phonograph.ui.fragments.mainactivity.library.pager;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;

import player.phonograph.R;
import player.phonograph.adapter.song.ShuffleButtonSongAdapter;
import player.phonograph.adapter.song.SongAdapter;
import player.phonograph.interfaces.LoaderIds;
import player.phonograph.loader.SongLoader;
import player.phonograph.misc.WrappedAsyncTaskLoader;
import player.phonograph.model.Song;
import player.phonograph.settings.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SongsFragment extends AbsLibraryPagerRecyclerViewCustomGridSizeFragment<SongAdapter, GridLayoutManager> implements LoaderManager.LoaderCallbacks<List<Song>> {

    private static final int LOADER_ID = LoaderIds.SONGS_FRAGMENT;

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
    protected SongAdapter createAdapter() {
        int itemLayoutRes = getItemLayoutRes();
        notifyLayoutResChanged(itemLayoutRes);
        boolean usePalette = loadUsePalette();
        List<Song> dataSet = getAdapter() == null ? new ArrayList<>() : getAdapter().getDataSet();

        if (getGridSize() <= getMaxGridSizeForList()) {
            return new ShuffleButtonSongAdapter(
                    getLibraryFragment().getMainActivity(),
                    dataSet,
                    itemLayoutRes,
                    usePalette,
                    getLibraryFragment());
        }
        return new SongAdapter(
                getLibraryFragment().getMainActivity(),
                dataSet,
                itemLayoutRes,
                usePalette,
                getLibraryFragment());
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.no_songs;
    }

    @Override
    public void onMediaStoreChanged() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    protected String loadSortOrder() {
        return Setting.instance().getSongSortOrder();
    }

    @Override
    protected void saveSortOrder(String sortOrder) {
        Setting.instance().setSongSortOrder(sortOrder);
    }

    @Override
    protected void setSortOrder(String sortOrder) {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    protected int loadGridSize() {
        return Setting.instance().getSongGridSize();
    }

    @Override
    protected void saveGridSize(int gridSize) {
        Setting.instance().setSongGridSize(gridSize);
    }

    @Override
    protected int loadGridSizeLand() {
        return Setting.instance().getSongGridSizeLand();
    }

    @Override
    protected void saveGridSizeLand(int gridSize) {
        Setting.instance().setSongGridSizeLand(gridSize);
    }

    @Override
    public void saveUsePalette(boolean usePalette) {
        Setting.instance().setSongColoredFooters(usePalette);
    }

    @Override
    public boolean loadUsePalette() {
        return Setting.instance().getSongColoredFooters();
    }

    @Override
    public void setUsePalette(boolean usePalette) {
        getAdapter().usePalette(usePalette);
    }

    @Override
    protected void setGridSize(int gridSize) {
        getLayoutManager().setSpanCount(gridSize);
        getAdapter().notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
        return new AsyncSongLoader(requireActivity());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
        getAdapter().swapDataSet(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        getAdapter().swapDataSet(new ArrayList<>());
    }

    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Song>> {
        public AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            return SongLoader.getAllSongs(getContext());
        }
    }
}
