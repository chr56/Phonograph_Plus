/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package util.phonograph.tageditor;

import static mt.util.color.MiscKt.resolveColor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableKt;

import org.jaudiotagger.tag.FieldKey;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import coil.Coil;
import coil.ImageLoader;
import coil.request.CachePolicy;
import coil.request.ImageRequest;
import kotlin.Unit;
import mt.util.color.ToolbarColor;
import player.phonograph.R;
import player.phonograph.coil.target.PaletteTargetBuilder;
import player.phonograph.databinding.ActivityAlbumTagEditorBinding;
import player.phonograph.mediastore.AlbumLoader;
import player.phonograph.model.Song;
import player.phonograph.util.ImageUtil;
import player.phonograph.util.PhonographColorUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import util.phonograph.lastfm.rest.LastFMRestClient;
import util.phonograph.lastfm.rest.LastFMUtil;
import util.phonograph.lastfm.rest.model.LastFmAlbum;

public class AlbumTagEditorActivity extends AbsTagEditorActivity implements TextWatcher {

    protected ActivityAlbumTagEditorBinding viewBinding;

    EditText albumTitle;
    EditText albumArtist;
    EditText genre;
    EditText year;

    private Bitmap albumArtBitmap;
    private boolean deleteAlbumArt;
    private LastFMRestClient lastFMRestClient;

    private final String TAG = "AlbumCoverImage";


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
        albumTitle.setText(model.getAlbumTitle());
        albumArtist.setText(model.getAlbumArtistName());
        genre.setText(model.getGenreName());
        year.setText(model.getSongYear());
    }

    @Override
    protected void loadCurrentImage() {
        Bitmap bitmap = model.getAlbumArt();
        if (bitmap != null) {
            setImageBitmap(
                    bitmap,
                    PhonographColorUtil.getColor(PhonographColorUtil.generatePalette(bitmap), resolveColor(this, R.attr.defaultFooterColor))
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
                        ImageLoader loader = Coil.imageLoader(AlbumTagEditorActivity.this);
                        ImageRequest request =
                                new ImageRequest.Builder(AlbumTagEditorActivity.this)
                                        .data(url)
                                        .target(
                                                new PaletteTargetBuilder(AlbumTagEditorActivity.this)
                                                        .onResourceReady((drawable, color) -> {
                                                            Bitmap bitmap = DrawableKt.toBitmap(drawable, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), null);
                                                            albumArtBitmap = ImageUtil.INSTANCE.resizeBitmap(bitmap, 2048);
                                                            setImageBitmap(albumArtBitmap, color);
                                                            deleteAlbumArt = false;
                                                            dataChanged();
                                                            setResult(RESULT_OK);
                                                            return Unit.INSTANCE;
                                                        })
                                                        .onFail(drawable -> {
                                                            Log.w(TAG, "Fail to load image cover:");
                                                            Log.i(TAG, "   Album      :" + albumTitleStr);
                                                            Log.i(TAG, "   AlbumArtist:" + albumArtistNameStr);
                                                            Log.i(TAG, "   Uri        :" + url);
                                                            return Unit.INSTANCE;
                                                        })
                                                        .build()
                                        )
                                        .build();
                        loader.enqueue(request);
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
        setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.default_album_art), resolveColor(this, R.attr.defaultFooterColor));
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

        writeValuesToFiles(fieldKeyValueMap, deleteAlbumArt ? new ArtworkInfo(model.getId(), null) : albumArtBitmap == null ? null : new ArtworkInfo(model.getId(), albumArtBitmap));
    }

    @Override
    protected View getContentViewLayout() {
        return viewBinding.getRoot();
    }

    @NonNull
    @Override
    protected List<String> getSongPaths() {
        List<Song> songs = AlbumLoader.INSTANCE.getAlbum(this, model.getId()).songs;
        List<String> paths = new ArrayList<>(songs.size());
        for (Song song : songs) {
            paths.add(song.data);
        }
        return paths;
    }

    @Override
    protected void loadImageFromFile(@NonNull final Uri selectedFileUri) {
        ImageLoader loader = Coil.imageLoader(AlbumTagEditorActivity.this);
        ImageRequest request = new ImageRequest.Builder(AlbumTagEditorActivity.this)
                .data(selectedFileUri)
                .diskCachePolicy(CachePolicy.DISABLED)
                .target(
                        new PaletteTargetBuilder(this)
                                .onResourceReady((drawable, color) -> {
                                    Bitmap bitmap = DrawableKt.toBitmap(drawable, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), null);
                                    albumArtBitmap = ImageUtil.INSTANCE.resizeBitmap(bitmap, 2048);
                                    setImageBitmap(albumArtBitmap, color);
                                    deleteAlbumArt = false;
                                    dataChanged();
                                    setResult(RESULT_OK);
                                    return Unit.INSTANCE;
                                })
                                .onFail(drawable -> {
                                    Log.w(TAG, "Fail to load image cover:");
                                    Log.i(TAG, "   Uri:  " + selectedFileUri);
                                    return Unit.INSTANCE;
                                })
                                .build()
                )
                .build();
        loader.enqueue(request);
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
        albumTitle.setTextColor(ToolbarColor.toolbarTitleColor(this, color));
    }
}
