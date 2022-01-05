package player.phonograph.ui.activities.tageditor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import org.jaudiotagger.tag.FieldKey;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import chr_56.MDthemer.util.ToolbarColorUtil;
import chr_56.MDthemer.util.Util;
import player.phonograph.R;
import player.phonograph.databinding.ActivityAlbumTagEditorBinding;
import player.phonograph.glide.SongGlideRequest;
import player.phonograph.glide.palette.BitmapPaletteWrapper;
import player.phonograph.lastfm.rest.LastFMRestClient;
import player.phonograph.lastfm.rest.model.LastFmAlbum;
import player.phonograph.loader.AlbumLoader;
import player.phonograph.model.Song;
import player.phonograph.util.ImageUtil;
import player.phonograph.util.LastFMUtil;
import player.phonograph.util.PhonographColorUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumTagEditorActivity extends AbsTagEditorActivity implements TextWatcher {

    protected ActivityAlbumTagEditorBinding viewBinding;

    EditText albumTitle;
    EditText albumArtist;
    EditText genre;
    EditText year;

    private Bitmap albumArtBitmap;
    private boolean deleteAlbumArt;
    private LastFMRestClient lastFMRestClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewBinding = ActivityAlbumTagEditorBinding.inflate(getLayoutInflater());
        bind();
        super.onCreate(savedInstanceState);

        lastFMRestClient = new LastFMRestClient(this);

        setUpViews();
    }

    private void bind() {
        albumTitle = viewBinding.title;
        albumArtist = viewBinding.albumArtist;
        genre = viewBinding.genre;
        year = viewBinding.year;
    }

    private void setUpViews() {
        fillViewsWithFileTags();
        albumTitle.addTextChangedListener(this);
        albumArtist.addTextChangedListener(this);
        genre.addTextChangedListener(this);
        year.addTextChangedListener(this);
    }


    private void fillViewsWithFileTags() {
        albumTitle.setText(getAlbumTitle());
        albumArtist.setText(getAlbumArtistName());
        genre.setText(getGenreName());
        year.setText(getSongYear());
    }

    @Override
    protected void loadCurrentImage() {
        Bitmap bitmap = getAlbumArt();
        if (bitmap != null) {
            setImageBitmap(
                    bitmap,
                    PhonographColorUtil.getColor(PhonographColorUtil.generatePalette(bitmap), Util.resolveColor(this, R.attr.defaultFooterColor))
            );
        }
        deleteAlbumArt = false;
    }

    @Override
    protected void getImageFromLastFM() {
        String albumTitleStr = albumTitle.getText().toString();
        String albumArtistNameStr = albumArtist.getText().toString();
        if (albumArtistNameStr.trim().equals("") || albumTitleStr.trim().equals("")) {
            Toast.makeText(this, getResources().getString(R.string.album_or_artist_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        lastFMRestClient.getApiService().getAlbumInfo(albumTitleStr, albumArtistNameStr, null).enqueue(new Callback<LastFmAlbum>() {
            @Override
            public void onResponse(@NonNull Call<LastFmAlbum> call, @NonNull Response<LastFmAlbum> response) {
                LastFmAlbum lastFmAlbum = response.body();
                if (lastFmAlbum.getAlbum() != null) {
                    String url = LastFMUtil.getLargestAlbumImageUrl(lastFmAlbum.getAlbum().getImage());
                    if (!TextUtils.isEmpty(url) && url.trim().length() > 0) {
                        Glide.with(AlbumTagEditorActivity.this)
                                .applyDefaultRequestOptions(SongGlideRequest.DEFAULT_OPTION)
                                .as(BitmapPaletteWrapper.class)
                                .load(url)
                                .listener(new RequestListener<BitmapPaletteWrapper>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<BitmapPaletteWrapper> target, boolean isFirstResource) {
                                        if (e != null) {
                                            e.printStackTrace();
                                        }
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(BitmapPaletteWrapper resource, Object model, Target<BitmapPaletteWrapper> target, DataSource dataSource, boolean isFirstResource) {
                                        return false;
                                    }
                                })
                                .into(new CustomTarget<BitmapPaletteWrapper>() {
                                    @Override
                                    public void onLoadFailed(Drawable errorDrawable) {
                                        super.onLoadFailed(errorDrawable);
                                    }

                                    @Override
                                    public void onResourceReady(@NonNull BitmapPaletteWrapper resource, @Nullable Transition<? super BitmapPaletteWrapper> transition) {
                                        albumArtBitmap = ImageUtil.resizeBitmap(resource.getBitmap(), 2048);
                                        setImageBitmap(albumArtBitmap, PhonographColorUtil.getColor(resource.getPalette(), Util.resolveColor(AlbumTagEditorActivity.this, R.attr.defaultFooterColor)));
                                        deleteAlbumArt = false;
                                        dataChanged();
                                        setResult(RESULT_OK);
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {
                                        // todo check leakage
                                    }
                                });
                        return;
                    }
                }
                toastLoadingFailed();
            }

            @Override
            public void onFailure(@NonNull Call<LastFmAlbum> call, @NonNull Throwable t) {
                toastLoadingFailed();
            }

            private void toastLoadingFailed() {
                Toast.makeText(AlbumTagEditorActivity.this,
                        R.string.could_not_download_album_cover, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void searchImageOnWeb() {
        searchWebFor(albumTitle.getText().toString(), albumArtist.getText().toString());
    }

    @Override
    protected void deleteImage() {
        setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.default_album_art), Util.resolveColor(this, R.attr.defaultFooterColor));
        deleteAlbumArt = true;
        dataChanged();
    }

    @Override
    protected void save() {
        Map<FieldKey, String> fieldKeyValueMap = new EnumMap<>(FieldKey.class);
        fieldKeyValueMap.put(FieldKey.ALBUM, albumTitle.getText().toString());
        //android seems not to recognize album_artist field so we additionally write the normal artist field
        fieldKeyValueMap.put(FieldKey.ARTIST, albumArtist.getText().toString());
        fieldKeyValueMap.put(FieldKey.ALBUM_ARTIST, albumArtist.getText().toString());
        fieldKeyValueMap.put(FieldKey.GENRE, genre.getText().toString());
        fieldKeyValueMap.put(FieldKey.YEAR, year.getText().toString());

        writeValuesToFiles(fieldKeyValueMap, deleteAlbumArt ? new ArtworkInfo(getId(), null) : albumArtBitmap == null ? null : new ArtworkInfo(getId(), albumArtBitmap));
    }

    @Override
    protected View getContentViewLayout() {
        return viewBinding.getRoot();
    }

    @NonNull
    @Override
    protected List<String> getSongPaths() {
        List<Song> songs = AlbumLoader.getAlbum(this, getId()).songs;
        List<String> paths = new ArrayList<>(songs.size());
        for (Song song : songs) {
            paths.add(song.data);
        }
        return paths;
    }

    @Override
    protected void loadImageFromFile(@NonNull final Uri selectedFileUri) {
        Glide.with(AlbumTagEditorActivity.this)
                .applyDefaultRequestOptions(SongGlideRequest.DEFAULT_OPTION)
                .as(BitmapPaletteWrapper.class)
                .skipMemoryCache(true)
                .load(selectedFileUri)
                .listener(new RequestListener<BitmapPaletteWrapper>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<BitmapPaletteWrapper> target, boolean isFirstResource) {
                        if (e != null) {
                            e.printStackTrace();
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(BitmapPaletteWrapper resource, Object model, Target<BitmapPaletteWrapper> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(new CustomTarget<BitmapPaletteWrapper>() {
                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                    }

                    @Override
                    public void onResourceReady(@NonNull BitmapPaletteWrapper resource, @Nullable Transition<? super BitmapPaletteWrapper> transition) {
                        PhonographColorUtil.getColor(resource.getPalette(), Color.TRANSPARENT);
                        albumArtBitmap = ImageUtil.resizeBitmap(resource.getBitmap(), 2048);
                        setImageBitmap(albumArtBitmap, PhonographColorUtil.getColor(resource.getPalette(), Util.resolveColor(AlbumTagEditorActivity.this, R.attr.defaultFooterColor)));
                        deleteAlbumArt = false;
                        dataChanged();
                        setResult(RESULT_OK);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // todo check leakage
                    }
                });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        dataChanged();
    }

    @Override
    protected void setColors(int color) {
        super.setColors(color);
        albumTitle.setTextColor(ToolbarColorUtil.toolbarTitleColor(this, color));
    }
}
