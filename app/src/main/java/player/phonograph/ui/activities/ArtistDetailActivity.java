package player.phonograph.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
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
import androidx.recyclerview.widget.LinearLayoutManager;
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

import util.mdcolor.pref.ThemeColor;
import util.mddesign.util.ColorUtil;
import util.mddesign.util.MaterialColorHelper;
import util.mddesign.util.ToolbarColorUtil;
import util.mddesign.util.Util;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import player.phonograph.R;
import player.phonograph.adapter.album.HorizontalAlbumAdapter;
import player.phonograph.adapter.song.ArtistSongAdapter;
import player.phonograph.databinding.ActivityArtistDetailBinding;
import player.phonograph.dialogs.AddToPlaylistDialog;
import player.phonograph.dialogs.SleepTimerDialog;
import player.phonograph.glide.ArtistGlideRequest;
import player.phonograph.glide.PhonographColoredTarget;
import player.phonograph.helper.MusicPlayerRemote;
import player.phonograph.interfaces.CabHolder;
import player.phonograph.interfaces.LoaderIds;
import player.phonograph.interfaces.PaletteColorHolder;
import player.phonograph.lastfm.rest.LastFMRestClient;
import player.phonograph.lastfm.rest.model.LastFmArtist;
import player.phonograph.loader.ArtistLoader;
import player.phonograph.misc.SimpleObservableScrollViewCallbacks;
import player.phonograph.misc.WrappedAsyncTaskLoader;
import player.phonograph.model.Artist;
import player.phonograph.model.Song;
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity;
import player.phonograph.util.CustomArtistImageUtil;
import player.phonograph.util.MusicUtil;
import player.phonograph.util.NavigationUtil;
import player.phonograph.util.PhonographColorUtil;
import player.phonograph.util.PreferenceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Be careful when changing things in this Activity!
 */
public class ArtistDetailActivity extends AbsSlidingMusicPanelActivity implements PaletteColorHolder, CabHolder, LoaderManager.LoaderCallbacks<Artist> {

    private static final int LOADER_ID = LoaderIds.ARTIST_DETAIL_ACTIVITY;
    private static final int REQUEST_CODE_SELECT_IMAGE = 1000;

    public static final String EXTRA_ARTIST_ID = "extra_artist_id";

    private ActivityArtistDetailBinding viewBinding;

    View songListHeader;
    RecyclerView albumRecyclerView;

    private AttachedCab cab;
    private int headerViewHeight;
    private int toolbarColor;

    private Artist artist;
    @Nullable
    private Spanned biography;
    private MaterialDialog biographyDialog;
    private HorizontalAlbumAdapter albumAdapter;
    private ArtistSongAdapter songAdapter;

    private LastFMRestClient lastFMRestClient;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewBinding = ActivityArtistDetailBinding.inflate(getLayoutInflater());

        super.onCreate(savedInstanceState);
        setDrawUnderStatusbar();

        lastFMRestClient = new LastFMRestClient(this);
        usePalette = PreferenceUtil.getInstance(this).albumArtistColoredFooters();

        initViews();
        setUpObservableListViewParams();
        setUpToolbar();
        setUpViews();

        getSupportLoaderManager().initLoader(LOADER_ID, getIntent().getExtras(), this);
    }

    @Override
    protected View createContentView() {
        return wrapSlidingMusicPanel(viewBinding.getRoot());
    }

    private boolean usePalette;

    private void setUpObservableListViewParams() {
        headerViewHeight = getResources().getDimensionPixelSize(R.dimen.detail_header_height);
    }

    private void initViews() {
        songListHeader = LayoutInflater.from(this).inflate(R.layout.artist_detail_header, viewBinding.list, false);
        albumRecyclerView = songListHeader.findViewById(R.id.recycler_view);
    }

    private void setUpViews() {
        setUpSongListView();
        setUpAlbumRecyclerView();
        setColors(Util.resolveColor(this, R.attr.defaultFooterColor));
    }

    private void setUpSongListView() {
        setUpSongListPadding();
        viewBinding.list.setScrollViewCallbacks(observableScrollViewCallbacks);
        viewBinding.list.addHeaderView(songListHeader);

        songAdapter = new ArtistSongAdapter(this, getArtist().getSongs(), this);
        viewBinding.list.setAdapter(songAdapter);

        final View contentView = getWindow().getDecorView().findViewById(android.R.id.content);
        contentView.post(() -> observableScrollViewCallbacks.onScrollChanged(-headerViewHeight, false, false));
    }

    private void setUpSongListPadding() {
        viewBinding.list.setPadding(0, headerViewHeight, 0, 0);
    }

    private void setUpAlbumRecyclerView() {
        albumRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        albumAdapter = new HorizontalAlbumAdapter(this, getArtist().albums, usePalette, this);
        albumRecyclerView.setAdapter(albumAdapter);
        albumAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (albumAdapter.getItemCount() == 0) finish();
            }
        });
    }

    protected void setUsePalette(boolean usePalette) {
        albumAdapter.usePalette(usePalette);
        PreferenceUtil.getInstance(this).setAlbumArtistColoredFooters(usePalette);
        this.usePalette = usePalette;
    }

    private void reload() {
        getSupportLoaderManager().restartLoader(LOADER_ID, getIntent().getExtras(), this);
    }

    private void loadBiography() {
        loadBiography(Locale.getDefault().getLanguage());
    }

    private void loadBiography(@Nullable final String lang) {
        biography = null;

        lastFMRestClient.getApiService()
                .getArtistInfo(getArtist().getName(), lang, null)
                .enqueue(new Callback<LastFmArtist>() {
                    @Override
                    public void onResponse(@NonNull Call<LastFmArtist> call, @NonNull Response<LastFmArtist> response) {
                        final LastFmArtist lastFmArtist = response.body();
                        if (lastFmArtist != null && lastFmArtist.getArtist() != null) {
                            final String bioContent = lastFmArtist.getArtist().getBio().getContent();
                            if (bioContent != null && !bioContent.trim().isEmpty()) {
                                biography = Html.fromHtml(bioContent);
                            }
                        }

                        // If the "lang" parameter is set and no biography is given, retry with default language
                        if (biography == null && lang != null) {
                            loadBiography(null);
                            return;
                        }

                        if (!PreferenceUtil.isAllowedToDownloadMetadata(ArtistDetailActivity.this)) {
                            if (biography != null) {
                                biographyDialog.message(null, biography, null);
                            } else {
                                biographyDialog.dismiss();
                                Toast.makeText(ArtistDetailActivity.this, getResources().getString(R.string.biography_unavailable), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LastFmArtist> call, @NonNull Throwable t) {
                        t.printStackTrace();
                        biography = null;
                    }
                });
    }

    private void loadArtistImage() {
        ArtistGlideRequest.Builder.from(Glide.with(this), artist)
                .generatePalette(this).build()
                .dontAnimate()
                .into(new PhonographColoredTarget(viewBinding.image) {
                    @Override
                    public void onColorReady(int color) {
                        setColors(color);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    CustomArtistImageUtil.getInstance(this).setCustomArtistImage(artist, data.getData());
                }
                break;
            default:
                if (resultCode == RESULT_OK) {
                    reload();
                }
                break;
        }
    }

    @Override
    public int getPaletteColor() {
        return toolbarColor;
    }

    private void setColors(int color) {
        toolbarColor = color;
        viewBinding.header.setBackgroundColor(color);

        setNavigationbarColor(color);
        setTaskDescriptionColor(color);

        viewBinding.toolbar.setBackgroundColor(color);
        setSupportActionBar(viewBinding.toolbar); // needed to auto readjust the toolbar content color
        viewBinding.toolbar.setTitleTextColor(ToolbarColorUtil.toolbarTitleColor(this, color));
        setStatusbarColor(color);

        int secondaryTextColor = MaterialColorHelper.getSecondaryTextColor(this, ColorUtil.isColorLight(color));

        ColorFilter f = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(secondaryTextColor, BlendModeCompat.SRC_IN);

        viewBinding.durationIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_timer_white_24dp));
        viewBinding.durationIcon.setColorFilter(f);
        viewBinding.songCountIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_music_note_white_24dp));
        viewBinding.songCountIcon.setColorFilter(f);
        viewBinding.albumCountIcon.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_album_white_24dp));
        viewBinding.albumCountIcon.setColorFilter(f);

        viewBinding.durationIcon.setColorFilter(secondaryTextColor, PorterDuff.Mode.SRC_IN);
        viewBinding.songCountIcon.setColorFilter(secondaryTextColor, PorterDuff.Mode.SRC_IN);
        viewBinding.albumCountIcon.setColorFilter(secondaryTextColor, PorterDuff.Mode.SRC_IN);

        viewBinding.durationText.setTextColor(secondaryTextColor);
        viewBinding.songCountText.setTextColor(secondaryTextColor);
        viewBinding.albumCountText.setTextColor(secondaryTextColor);
    }

    private void setUpToolbar() {
        setSupportActionBar(viewBinding.toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_artist_detail, menu);
        menu.findItem(R.id.action_colored_footers).setChecked(usePalette);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        final List<Song> songs = songAdapter.getDataSet();
        switch (id) {
            case R.id.action_sleep_timer:
                new SleepTimerDialog().show(getSupportFragmentManager(), "SET_SLEEP_TIMER");
                return true;
            case R.id.action_equalizer:
                NavigationUtil.openEqualizer(this);
                return true;
            case R.id.action_shuffle_artist:
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
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.action_biography:
                if (biographyDialog == null) {
                    biographyDialog = new MaterialDialog(this, MaterialDialog.getDEFAULT_BEHAVIOR())
                            .title(null, artist.getName())
                            .positiveButton(android.R.string.ok, null, null);
                    //set button color
                    DialogActionExtKt.getActionButton(biographyDialog, WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(this));
                }
                if (PreferenceUtil.isAllowedToDownloadMetadata(ArtistDetailActivity.this)) { // wiki should've been already downloaded
                    if (biography != null) {
                        biographyDialog.message(null, biography, null);
                        biographyDialog.show();
                    } else {
                        Toast.makeText(ArtistDetailActivity.this, getResources().getString(R.string.biography_unavailable), Toast.LENGTH_SHORT).show();
                    }
                } else { // force download
                    biographyDialog.show();
                    loadBiography();
                }
                return true;
            case R.id.action_set_artist_image:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_from_local_storage)), REQUEST_CODE_SELECT_IMAGE);
                return true;
            case R.id.action_reset_artist_image:
                Toast.makeText(ArtistDetailActivity.this, getResources().getString(R.string.updating), Toast.LENGTH_SHORT).show();
                CustomArtistImageUtil.getInstance(ArtistDetailActivity.this).resetCustomArtistImage(artist);
                return true;
            case R.id.action_colored_footers:
                item.setChecked(!item.isChecked());
                setUsePalette(item.isChecked());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    public AttachedCab showCab(int menuRes,
                               @NonNull Function2<? super AttachedCab, ? super Menu, Unit> createCallback,
                               @NonNull Function1<? super MenuItem, Boolean> selectCallback,
                               @NonNull Function1<? super AttachedCab, Boolean> destroyCallback) {

        if (cab != null && AttachedCabKt.isActive(cab)) AttachedCabKt.destroy(cab);

        cab = MaterialCabKt.createCab(this, R.id.cab_stub, attachedCab -> {
            attachedCab.popupTheme(PreferenceUtil.getInstance(this).getGeneralTheme());
            attachedCab.menu(menuRes);
            attachedCab.closeDrawable(R.drawable.ic_close_white_24dp);
            attachedCab.backgroundColor(null, PhonographColorUtil.shiftBackgroundColorForLightText(getPaletteColor()));
            attachedCab.onCreate(createCallback);
            attachedCab.onSelection(selectCallback);
            attachedCab.onDestroy(destroyCallback);
            return null;
        });

        return cab;
    }

    @Override
    public void onBackPressed() {
        if (cab != null && AttachedCabKt.isActive(cab)) AttachedCabKt.destroy(cab);
        else {
            albumRecyclerView.stopScroll();
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

    private void setArtist(Artist artist) {
        this.artist = artist;
        loadArtistImage();

        if (PreferenceUtil.isAllowedToDownloadMetadata(this)) {
            loadBiography();
        }

        getSupportActionBar().setTitle(artist.getName());
        viewBinding.songCountText.setText(MusicUtil.getSongCountString(this, artist.getSongCount()));
        viewBinding.albumCountText.setText(MusicUtil.getAlbumCountString(this, artist.getAlbumCount()));
        viewBinding.durationText.setText(MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(this, artist.getSongs())));

        songAdapter.swapDataSet(artist.getSongs());
        albumAdapter.swapDataSet(artist.albums);
    }

    private Artist getArtist() {
        if (artist == null) artist = new Artist();
        return artist;
    }

    @Override
    public Loader<Artist> onCreateLoader(int id, Bundle args) {
        return new AsyncArtistDataLoader(this, args.getLong(EXTRA_ARTIST_ID));
    }

    @Override
    public void onLoadFinished(Loader<Artist> loader, Artist data) {
        setArtist(data);
    }

    @Override
    public void onLoaderReset(Loader<Artist> loader) {
        this.artist = new Artist();
        songAdapter.swapDataSet(artist.getSongs());
        albumAdapter.swapDataSet(artist.albums);
    }

    private static class AsyncArtistDataLoader extends WrappedAsyncTaskLoader<Artist> {
        private final long artistId;

        public AsyncArtistDataLoader(Context context, long artistId) {
            super(context);
            this.artistId = artistId;
        }

        @Override
        public Artist loadInBackground() {
            return ArtistLoader.getArtist(getContext(), artistId);
        }
    }
}
