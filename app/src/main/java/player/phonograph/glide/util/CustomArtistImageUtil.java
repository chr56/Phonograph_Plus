/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.glide.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;

import player.phonograph.App;
import player.phonograph.glide.SongGlideRequest;
import player.phonograph.model.Artist;
import player.phonograph.notification.ErrorNotification;

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
                .load(uri)
                .listener(
                        new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                Log.w(TAG, "Fail to load artist cover:");
                                Log.i(TAG, "   Artist" + artist.getName() + " " + artist.getId());
                                Log.i(TAG, "   Uri:  " + uri.toString());
                                ErrorNotification.INSTANCE.postErrorNotification(e, "Fail to save custom artist image");
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                CustomArtistImageUtilKt.INSTANCE.saveCustomArtistImage(App.getInstance(), artist, resource);
                                return false;
                            }
                        }
                )
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }

                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    }

                });
    }

    // shared prefs saves us many IO operations
    public boolean hasCustomArtistImage(Artist artist) {
        return mPreferences.getBoolean(CustomArtistImageUtilKt.INSTANCE.getArtistFileName(artist), false);
    }

    public static File getFile(Artist artist) {
        File dir = new File(App.getInstance().getFilesDir(), FOLDER_NAME);
        return new File(dir, CustomArtistImageUtilKt.INSTANCE.getArtistFileName(artist));
    }
}
