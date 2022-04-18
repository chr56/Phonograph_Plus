package player.phonograph.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.ColorFilter;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialcab.MaterialCabKt;
import com.afollestad.materialcab.attached.AttachedCab;
import com.afollestad.materialcab.attached.AttachedCabKt;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.WhichButton;
import com.afollestad.materialdialogs.actions.DialogActionExtKt;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import player.phonograph.R;
import player.phonograph.adapter.song.AlbumSongAdapter;
import player.phonograph.databinding.ActivityAlbumDetailBinding;
import player.phonograph.dialogs.AddToPlaylistDialog;
import player.phonograph.dialogs.DeleteSongsDialog;
import player.phonograph.dialogs.SleepTimerDialog;
import player.phonograph.glide.PhonographColoredTarget;
import player.phonograph.glide.SongGlideRequest;
import player.phonograph.service.MusicPlayerRemote;
import player.phonograph.interfaces.CabHolder;
import player.phonograph.interfaces.LoaderIds;
import player.phonograph.interfaces.PaletteColorHolder;
import util.phonograph.lastfm.rest.LastFMRestClient;
import util.phonograph.lastfm.rest.model.LastFmAlbum;
import player.phonograph.loader.AlbumLoader;
import player.phonograph.misc.SimpleObservableScrollViewCallbacks;
import player.phonograph.misc.WrappedAsyncTaskLoader;
import player.phonograph.model.Album;
import player.phonograph.model.Song;
import player.phonograph.settings.Setting;
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity;
import util.phonograph.tageditor.AbsTagEditorActivity;
import util.phonograph.tageditor.AlbumTagEditorActivity;
import player.phonograph.util.MusicUtil;
import player.phonograph.util.NavigationUtil;
import player.phonograph.util.PhonographColorUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import util.mdcolor.pref.ThemeColor;
import util.mddesign.core.Themer;
import util.mdcolor.ColorUtil;
import util.mddesign.util.MaterialColorHelper;
import util.mddesign.util.Util;

/**
 * Be careful when changing things in this Activity!
 */
public class AlbumDetailActivity extends AbsSlidingMusicPanelActivity implements PaletteColorHolder, CabHolder, LoaderManager.LoaderCallbacks<Album> {

    private static final int TAG_EDITOR_REQUEST = 2001;
    private static final int LOADER_ID = LoaderIds.ALBUM_DETAIL_ACTIVITY;

    public static final String EXTRA_ALBUM_ID = "extra_album_id";

    private Album album;

    private ActivityAlbumDetailBinding viewBinding;


    private AlbumSongAdapter adapter;

    private AttachedCab cab;
    private int headerViewHeight;
    private int toolbarColor;

    @Nullable
    private Spanned wiki;
    private MaterialDialog wikiDialog;
    private LastFMRestClient lastFMRestClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewBinding = ActivityAlbumDetailBinding.inflate(getLayoutInflater());

        super.onCreate(savedInstanceState);
        setDrawUnderStatusbar();

        Themer.setActivityToolbarColorAuto(this, viewBinding.toolbar);

        lastFMRestClient = new LastFMRestClient(this);

        setUpObservableListViewParams();
        setUpToolBar();
        setUpViews();

        getSupportLoaderManager().initLoader(LOADER_ID, getIntent().getExtras(), this);
    }


    @Override
    protected View createContentView() {
        return wrapSlidingMusicPanel(viewBinding.getRoot());
    }

    private final SimpleObservableScrollViewCallbacks observableScrollViewCallbacks = new SimpleObservableScrollViewCallbacks() {
        @Override
        public void onScrollChanged(int scrollY, boolean b, boolean b2) {
            scrollY += headerViewHeight;

            // Change alpha of overlay
            float headerAlpha = Math.max(0, Math.min(1, (float) 2 * scrollY / headerViewHeight));
            viewBinding.headerOverlay.setBackgroundColor(ColorUtil.withAlpha(toolbarColor, headerAlpha));

            // Translate name text
            viewBinding.header.setTranslationY(Math.max(-scrollY, -headerViewHeight));
            viewBinding.headerOverlay.setTranslationY(Math.max(-scrollY, -headerViewHeight));
            viewBinding.image.setTranslationY(Math.max(-scrollY, -headerViewHeight));
        }
    };

    private void setUpObservableListViewParams() {
        headerViewHeight = getResources().getDimensionPixelSize(R.dimen.detail_header_height);
    }

    private void setUpViews() {
        setUpRecyclerView();
        setUpSongsAdapter();
        viewBinding.artistText.setOnClickListener(v -> {
            if (album != null) {
                NavigationUtil.goToArtist(AlbumDetailActivity.this, album.getArtistId());
            }
        });
        setColors(Util.resolveColor(this, R.attr.defaultFooterColor));
    }

    private void loadAlbumCover() {
        SongGlideRequest.Builder.from(Glide.with(this), getAlbum().safeGetFirstSong())
                .checkIgnoreMediaStore(this)
                .generatePalette(this).build()
                .dontAnimate()
                .into(new PhonographColoredTarget(viewBinding.image) {
                    @Override
                    public void onColorReady(int color) {
                        setColors(color);
                    }
                });
    }

    private void setColors(int color) {
        toolbarColor = color;
        viewBinding.header.setBackgroundColor(color);

        setNavigationbarColor(color);
        setTaskDescriptionColor(color);

        viewBinding.toolbar.setBackgroundColor(color);
        setSupportActionBar(viewBinding.toolbar); // needed to auto readjust the toolbar content color
        setStatusbarColor(color);

        int secondaryTextColor = MaterialColorHelper.getSecondaryTextColor(this, ColorUtil.isColorLight(color));


        ColorFilter f = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(secondaryTextColor, BlendModeCompat.SRC_IN);

        viewBinding.artistIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_person_white_24dp));
        viewBinding.artistIcon.setColorFilter(f);
        viewBinding.durationIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_timer_white_24dp));
        viewBinding.durationIcon.setColorFilter(f);
        viewBinding.songCountIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_music_note_white_24dp));
        viewBinding.songCountIcon.setColorFilter(f);
        viewBinding.albumYearIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_event_white_24dp));
        viewBinding.albumYearIcon.setColorFilter(f);

        viewBinding.artistText.setTextColor(MaterialColorHelper.getPrimaryTextColor(this, ColorUtil.isColorLight(color)));
        viewBinding.durationText.setTextColor(secondaryTextColor);
        viewBinding.songCountText.setTextColor(secondaryTextColor);
        viewBinding.albumYearText.setTextColor(secondaryTextColor);
    }

    @Override
    public int getPaletteColor() {
        return toolbarColor;
    }

    private void setUpRecyclerView() {
        setUpRecyclerViewPadding();
        viewBinding.list.setScrollViewCallbacks(observableScrollViewCallbacks);
        final View contentView = getWindow().getDecorView().findViewById(android.R.id.content);
        contentView.post(() -> observableScrollViewCallbacks.onScrollChanged(-headerViewHeight, false, false));
    }

    private void setUpRecyclerViewPadding() {
        viewBinding.list.setPadding(0, headerViewHeight, 0, 0);
    }

    private void setUpToolBar() {
        setSupportActionBar(viewBinding.toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setUpSongsAdapter() {
        adapter = new AlbumSongAdapter(this, getAlbum().songs, R.layout.item_list, false, this);
        viewBinding.list.setLayoutManager(new GridLayoutManager(this, 1));
        viewBinding.list.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (adapter.getItemCount() == 0) finish();
            }
        });
    }

    private void reload() {
        getSupportLoaderManager().restartLoader(LOADER_ID, getIntent().getExtras(), this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_album_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void loadWiki() {
        loadWiki(Locale.getDefault().getLanguage());
    }

    private void loadWiki(@Nullable final String lang) {
        wiki = null;

        lastFMRestClient.getApiService()
                .getAlbumInfo(getAlbum().getTitle(), getAlbum().getArtistName(), lang)
                .enqueue(new Callback<LastFmAlbum>() {
                    @Override
                    public void onResponse(@NonNull Call<LastFmAlbum> call, @NonNull Response<LastFmAlbum> response) {
                        final LastFmAlbum lastFmAlbum = response.body();
                        if (lastFmAlbum != null && lastFmAlbum.getAlbum() != null && lastFmAlbum.getAlbum().getWiki() != null) {
                            final String wikiContent = lastFmAlbum.getAlbum().getWiki().getContent();
                            if (wikiContent != null && !wikiContent.trim().isEmpty()) {
                                wiki = Html.fromHtml(wikiContent);
                            }
                        }

                        // If the "lang" parameter is set and no wiki is given, retry with default language
                        if (wiki == null && lang != null) {
                            loadWiki(null);
                            return;
                        }

                        if (!Setting.isAllowedToDownloadMetadata(AlbumDetailActivity.this)) {
                            if (wiki != null) {
                                wikiDialog.message(null, wiki, null);
                            } else {
                                wikiDialog.dismiss();
                                Toast.makeText(AlbumDetailActivity.this, getResources().getString(R.string.wiki_unavailable), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LastFmAlbum> call, @NonNull Throwable t) {
                        t.printStackTrace();
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        final List<Song> songs = adapter.getDataSet();
        switch (id) {
            case R.id.action_sleep_timer:
                new SleepTimerDialog().show(getSupportFragmentManager(), "SET_SLEEP_TIMER");
                return true;
            case R.id.action_equalizer:
                NavigationUtil.openEqualizer(this);
                return true;
            case R.id.action_shuffle_album:
                MusicPlayerRemote.openAndShuffleQueue(songs, true);
                return true;
            case R.id.action_play_next:
                MusicPlayerRemote.playNext(songs);
                return true;
            case R.id.action_add_to_current_playing:
                MusicPlayerRemote.enqueue(songs);
                return true;
            case R.id.action_add_to_playlist:
                AddToPlaylistDialog.create(songs).show(getSupportFragmentManager(), "ADD_PLAYLIST");
                return true;
            case R.id.action_delete_from_device:
                DeleteSongsDialog.create(songs).show(getSupportFragmentManager(), "DELETE_SONGS");
                return true;
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.action_tag_editor:
                Intent intent = new Intent(this, AlbumTagEditorActivity.class);
                intent.putExtra(AbsTagEditorActivity.EXTRA_ID, getAlbum().getId());
                startActivityForResult(intent, TAG_EDITOR_REQUEST);
                return true;
            case R.id.action_go_to_artist:
                NavigationUtil.goToArtist(this, getAlbum().getArtistId());
                return true;
            case R.id.action_wiki:
                if (wikiDialog == null) {
                    wikiDialog = new MaterialDialog(this, MaterialDialog.getDEFAULT_BEHAVIOR())
                            .title(null, album.getTitle())
                            .positiveButton(android.R.string.ok, null, null);
                    // set button color
                    DialogActionExtKt.getActionButton(wikiDialog, WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(this));

                }
                if (Setting.isAllowedToDownloadMetadata(this)) {
                    if (wiki != null) {
                        wikiDialog.message(null, wiki, null);
                        wikiDialog.show();
                    } else {
                        Toast.makeText(this, getResources().getString(R.string.wiki_unavailable), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    wikiDialog.show();
                    loadWiki();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAG_EDITOR_REQUEST) {
            reload();
            setResult(RESULT_OK);
        }
    }

    @NonNull
    @Override
    public AttachedCab showCab(int menuRes,
                               @NonNull Function2<? super AttachedCab, ? super Menu, Unit> createCallback,
                               @NonNull Function1<? super MenuItem, Boolean> selectCallback,
                               @NonNull Function1<? super AttachedCab, Boolean> destroyCallback) {

        if (cab != null && AttachedCabKt.isActive(cab)) AttachedCabKt.destroy(cab);

        cab = MaterialCabKt.createCab(this, R.id.cab_stub, attachedCab -> {
            attachedCab.popupTheme(Setting.instance().getGeneralTheme());
            attachedCab.menu(menuRes);
            attachedCab.closeDrawable(R.drawable.ic_close_white_24dp);
            attachedCab.backgroundColor(null,PhonographColorUtil.shiftBackgroundColorForLightText(getPaletteColor()));
            attachedCab.onCreate(createCallback);
            attachedCab.onSelection(selectCallback);
            attachedCab.onDestroy(destroyCallback);
            return null;
        }

        );
        return cab;
    }

    @Override
    public void onBackPressed() {
        if (cab != null && AttachedCabKt.isActive(cab)) AttachedCabKt.destroy(cab);
        else {
            viewBinding.list.stopScroll();
            super.onBackPressed();
        }
    }

    @Override
    public void onMediaStoreChanged() {
        super.onMediaStoreChanged();
        reload();
    }

    @Override
    public void setStatusbarColor(int color) {
        super.setStatusbarColor(color);
        setLightStatusbar(false);
    }

    private void setAlbum(Album album) {
        this.album = album;
        loadAlbumCover();

        if (Setting.isAllowedToDownloadMetadata(this)) {
            loadWiki();
        }

        getSupportActionBar().setTitle(album.getTitle());
        viewBinding.artistText.setText(album.getArtistName());
        viewBinding.songCountText.setText(MusicUtil.getSongCountString(this, album.getSongCount()));
        viewBinding.durationText.setText(MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(this, album.songs)));
        viewBinding.albumYearText.setText(MusicUtil.getYearString(album.getYear()));

        adapter.setDataSet(album.songs);
    }

    private Album getAlbum() {
        if (album == null) album = new Album();
        return album;
    }

    @Override
    public Loader<Album> onCreateLoader(int id, Bundle args) {
        return new AsyncAlbumLoader(this, args.getLong(EXTRA_ALBUM_ID));
    }

    @Override
    public void onLoadFinished(Loader<Album> loader, Album data) {
        setAlbum(data);
    }

    @Override
    public void onLoaderReset(Loader<Album> loader) {
        this.album = new Album();
        adapter.setDataSet(album.songs);
    }

    private static class AsyncAlbumLoader extends WrappedAsyncTaskLoader<Album> {
        private final long albumId;

        public AsyncAlbumLoader(Context context, long albumId) {
            super(context);
            this.albumId = albumId;
        }

        @Override
        public Album loadInBackground() {
            return AlbumLoader.getAlbum(getContext(), albumId);
        }
    }
}
