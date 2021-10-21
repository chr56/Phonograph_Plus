package com.kabouzeid.phonograph.glide;

import android.content.Context;

import java.io.InputStream;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.module.GlideModule;
import com.kabouzeid.phonograph.glide.artistimage.ArtistImage;
import com.kabouzeid.phonograph.glide.artistimage.ArtistImageLoader;
import com.kabouzeid.phonograph.glide.audiocover.AudioFileCover;
import com.kabouzeid.phonograph.glide.audiocover.AudioFileCoverLoader;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PhonographGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        glide.register(AudioFileCover.class, InputStream.class, new AudioFileCoverLoader.Factory());
        glide.register(ArtistImage.class, InputStream.class, new ArtistImageLoader.Factory());
    }
}
