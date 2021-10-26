package player.phonograph.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialcab.MaterialCab;
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;

import player.phonograph.App;
import player.phonograph.R;
import player.phonograph.Task;
import player.phonograph.TaskManager;
import player.phonograph.adapter.song.OrderablePlaylistSongAdapter;
import player.phonograph.adapter.song.PlaylistSongAdapter;
import player.phonograph.adapter.song.SongAdapter;
import player.phonograph.util.FileSaver;
import player.phonograph.helper.MusicPlayerRemote;
import player.phonograph.helper.menu.PlaylistMenuHelper;
import player.phonograph.interfaces.CabHolder;
import player.phonograph.interfaces.LoaderIds;
import player.phonograph.loader.PlaylistLoader;
import player.phonograph.loader.PlaylistSongLoader;
import player.phonograph.misc.WrappedAsyncTaskLoader;
import player.phonograph.model.AbsCustomPlaylist;
import player.phonograph.model.Playlist;
import player.phonograph.model.Song;
import player.phonograph.model.smartplaylist.HistoryPlaylist;
import player.phonograph.model.smartplaylist.LastAddedPlaylist;
import player.phonograph.model.smartplaylist.MyTopTracksPlaylist;
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity;
import player.phonograph.util.PhonographColorUtil;
import player.phonograph.util.PlaylistsUtil;
import player.phonograph.util.ViewUtil;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import chr_56.MDthemer.core.ThemeColor;

public class PlaylistDetailActivity extends AbsSlidingMusicPanelActivity implements CabHolder, LoaderManager.LoaderCallbacks<List<Song>> {

    private static final int LOADER_ID = LoaderIds.PLAYLIST_DETAIL_ACTIVITY;
    private static final String TAG = "PlaylistDetail";

    @NonNull
    public static String EXTRA_PLAYLIST = "extra_playlist";

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(android.R.id.empty)
    TextView empty;

    private Playlist playlist;

    private MaterialCab cab;
    private SongAdapter adapter;

    private RecyclerView.Adapter wrappedAdapter;
    private RecyclerViewDragDropManager recyclerViewDragDropManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDrawUnderStatusbar();
        ButterKnife.bind(this);

        setStatusbarColorAuto();
        setNavigationbarColorAuto();
        setTaskDescriptionColorAuto();

        playlist = getIntent().getExtras().getParcelable(EXTRA_PLAYLIST);

        setUpRecyclerView();

        setUpToolbar();

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    protected View createContentView() {
        return wrapSlidingMusicPanel(R.layout.activity_playlist_detail);
    }

    private void setUpRecyclerView() {
        ViewUtil.setUpFastScrollRecyclerViewColor(this, ((FastScrollRecyclerView) recyclerView), ThemeColor.accentColor(this));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        if (playlist instanceof AbsCustomPlaylist) {
            adapter = new PlaylistSongAdapter(this, new ArrayList<>(), R.layout.item_list, false, this);
            recyclerView.setAdapter(adapter);
        } else {
            recyclerViewDragDropManager = new RecyclerViewDragDropManager();
            final GeneralItemAnimator animator = new RefactoredDefaultItemAnimator();
            adapter = new OrderablePlaylistSongAdapter(this, new ArrayList<>(), R.layout.item_list, false, this, (fromPosition, toPosition) -> {
                if (PlaylistsUtil.moveItem(PlaylistDetailActivity.this, playlist.id, fromPosition, toPosition)) {
                    Song song = adapter.getDataSet().remove(fromPosition);
                    adapter.getDataSet().add(toPosition, song);
                    adapter.notifyItemMoved(fromPosition, toPosition);
                }
            });
            wrappedAdapter = recyclerViewDragDropManager.createWrappedAdapter(adapter);

            recyclerView.setAdapter(wrappedAdapter);
            recyclerView.setItemAnimator(animator);

            recyclerViewDragDropManager.attachRecyclerView(recyclerView);
        }

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkIsEmpty();
            }
        });
    }

    private void setUpToolbar() {
        toolbar.setBackgroundColor(ThemeColor.primaryColor(this));
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarTitle(playlist.name);
    }

    private void setToolbarTitle(String title) {
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(playlist instanceof AbsCustomPlaylist ? R.menu.menu_smart_playlist_detail : R.menu.menu_playlist_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_shuffle_playlist:
                MusicPlayerRemote.openAndShuffleQueue(adapter.getDataSet(), true);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return PlaylistMenuHelper.handleMenuClick(this, playlist, item);
    }

    @NonNull
    @Override
    public MaterialCab openCab(final int menu, final MaterialCab.Callback callback) {
        if (cab != null && cab.isActive()) cab.finish();
        cab = new MaterialCab(this, R.id.cab_stub)
                .setMenu(menu)
                .setCloseDrawableRes(R.drawable.ic_close_white_24dp)
                .setBackgroundColor(PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(this)))
                .start(callback);
        return cab;
    }

    @Override
    public void onBackPressed() {
        if (cab != null && cab.isActive()) cab.finish();
        else {
            recyclerView.stopScroll();
            super.onBackPressed();
        }
    }

    @Override
    public void onMediaStoreChanged() {
        super.onMediaStoreChanged();

        if (!(playlist instanceof AbsCustomPlaylist)) {
            // Playlist deleted
            if (!PlaylistsUtil.doesPlaylistExist(this, playlist.id)) {
                finish();
                return;
            }

            // Playlist renamed
            final String playlistName = PlaylistsUtil.getNameForPlaylist(this, playlist.id);
            if (!playlistName.equals(playlist.name)) {
                playlist = PlaylistLoader.getPlaylist(this, playlist.id);
                setToolbarTitle(playlist.name);
            }
        }

        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    private void checkIsEmpty() {
        empty.setVisibility(
                adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE
        );
    }

    @Override
    public void onPause() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager.cancelDrag();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (recyclerViewDragDropManager != null) {
            recyclerViewDragDropManager.release();
            recyclerViewDragDropManager = null;
        }

        if (recyclerView != null) {
            recyclerView.setItemAnimator(null);
            recyclerView.setAdapter(null);
            recyclerView = null;
        }

        if (wrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(wrappedAdapter);
            wrappedAdapter = null;
        }
        adapter = null;

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == 100_000){
            if (data != null) {
                Uri uri = data.getData();
                Log.d(TAG,"SAF: " + uri.toString());

                Task task = App.getInstance().getTaskManager().findTask(requestCode);
                if (task != null){
                    if (Objects.equals(task.getAction(), Task.SAVE_PLAYLIST)) {
                        try {
                            String _data = task.getData();
                            if (_data != null){
                                short result = -1;
                                switch (_data){
                                    case "Normal":
                                        if (task.getNum() != null) {
                                            result = FileSaver.INSTANCE.savePlaylist(this, uri, task.getNum());
                                        }
                                        break;
                                    case "MyTopTracksPlaylist":
                                        result = FileSaver.INSTANCE.savePlaylist(this, uri, new MyTopTracksPlaylist(this));
                                        break;
                                    case "LastAddedPlaylist":
                                        result = FileSaver.INSTANCE.savePlaylist(this, uri, new LastAddedPlaylist(this));
                                        break;
                                    case "HistoryPlaylist":
                                        result = FileSaver.INSTANCE.savePlaylist(this, uri, new HistoryPlaylist(this));
                                        break;
                                    default:
                                        //
                                }
                                if (result != 0) {
                                    Toast.makeText(this, getResources().getText(R.string.failed_to_save_playlist, "_"), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, getResources().getText(R.string.success), Toast.LENGTH_SHORT).show();
                                }
                                Log.w(TAG, "result"+ result);

                            }




                        } finally {
                        }
                    }
                }

            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
        return new AsyncPlaylistSongLoader(this, playlist);
    }

    @Override
    public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
        if (adapter != null)
            adapter.swapDataSet(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Song>> loader) {
        if (adapter != null)
            adapter.swapDataSet(new ArrayList<>());
    }

    private static class AsyncPlaylistSongLoader extends WrappedAsyncTaskLoader<List<Song>> {
        private final Playlist playlist;

        public AsyncPlaylistSongLoader(Context context, Playlist playlist) {
            super(context);
            this.playlist = playlist;
        }

        @Override
        public List<Song> loadInBackground() {
            if (playlist instanceof AbsCustomPlaylist) {
                return ((AbsCustomPlaylist) playlist).getSongs(getContext());
            } else {
                //noinspection unchecked
                return (List) PlaylistSongLoader.getPlaylistSongList(getContext(), playlist.id);
            }
        }
    }
}
