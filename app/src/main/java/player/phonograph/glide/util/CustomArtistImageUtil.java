/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.glide.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import player.phonograph.App;
import player.phonograph.glide.SongGlideRequest;
import player.phonograph.model.Artist;
import player.phonograph.util.ImageUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

public class CustomArtistImageUtil {
    private static final String CUSTOM_ARTIST_IMAGE_PREFS = "custom_artist_image";
    private static final String FOLDER_NAME = "/custom_artist_images/";

    private final String TAG = "ArtistCoverImage";

    private static CustomArtistImageUtil sInstance;

    private final SharedPreferences mPreferences;

    private CustomArtistImageUtil(@NonNull final Context context) {
        mPreferences = context.getApplicationContext().getSharedPreferences(CUSTOM_ARTIST_IMAGE_PREFS, Context.MODE_PRIVATE);
    }

    public static CustomArtistImageUtil getInstance(@NonNull final Context context) {
        if (sInstance == null) {
            sInstance = new CustomArtistImageUtil(context.getApplicationContext());
        }
        return sInstance;
    }

    public void setCustomArtistImage(final Artist artist, Uri uri) {
        Glide.with(App.getInstance())
                .asBitmap()
                .apply(SongGlideRequest.DEFAULT_OPTION)
                .skipMemoryCache(true)
                .listener(
                        new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                Log.w(TAG, "Fail to load artist cover:");
                                Log.i(TAG, "   Artist" + artist.getName() + " " + artist.getId());
                                Log.i(TAG, "   Uri:  " + uri.toString());
//                                e.printStackTrace();
                                // todo send notification instead
//                              Toast.makeText(App.getInstance(), e.toString(), Toast.LENGTH_LONG).show();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }
                        }
                )
                .load(uri)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // todo check leakage
                    }

                    @SuppressLint("StaticFieldLeak")
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        new AsyncTask<Void, Void, Void>() {
                            @SuppressLint("ApplySharedPref")
                            @Override
                            protected Void doInBackground(Void... params) {
                                File dir = new File(App.getInstance().getFilesDir(), FOLDER_NAME);
                                if (!dir.exists()) {
                                    if (!dir.mkdirs()) { // create the folder
                                        return null;
                                    }
                                }
                                File file = new File(dir, getFileName(artist));

                                boolean succesful = false;
                                try {
                                    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                                    succesful = ImageUtil.INSTANCE.resizeBitmap(resource, 2048).compress(Bitmap.CompressFormat.JPEG, 100, os);
                                    os.close();
                                } catch (IOException e) {
                                    Toast.makeText(App.getInstance(), e.toString(), Toast.LENGTH_LONG).show();
                                }

                                if (succesful) {
                                    mPreferences.edit().putBoolean(getFileName(artist), true).commit();
                                    ArtistSignatureUtil.getInstance(App.getInstance()).updateArtistSignature(artist.getName());
                                    App.getInstance().getContentResolver().notifyChange(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, null); // trigger media store changed to force artist image reload
                                }
                                return null;
                            }
                        }.execute();
                    }

                });
    }

    @SuppressLint("StaticFieldLeak")
    public void resetCustomArtistImage(final Artist artist) {
        new AsyncTask<Void, Void, Void>() {
            @SuppressLint("ApplySharedPref")
            @Override
            protected Void doInBackground(Void... params) {
                mPreferences.edit().putBoolean(getFileName(artist), false).commit();
                ArtistSignatureUtil.getInstance(App.getInstance()).updateArtistSignature(artist.getName());
                App.getInstance().getContentResolver().notifyChange(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, null); // trigger media store changed to force artist image reload

                File file = getFile(artist);
                if (!file.exists()) {
                    return null;
                } else {
                    file.delete();
                }
                return null;
            }
        }.execute();
    }

    // shared prefs saves us many IO operations
    public boolean hasCustomArtistImage(Artist artist) {
        return mPreferences.getBoolean(getFileName(artist), false);
    }

    private static String getFileName(Artist artist) {
        String artistName = artist.getName();
        if (artistName == null)
            artistName = "";
        // replace everything that is not a letter or a number with _
        artistName = artistName.replaceAll("[^a-zA-Z0-9]", "_");
        return String.format(Locale.US, "#%d#%s.jpeg", artist.getId(), artistName);
    }

    public static File getFile(Artist artist) {
        File dir = new File(App.getInstance().getFilesDir(), FOLDER_NAME);
        return new File(dir, getFileName(artist));
    }
}
