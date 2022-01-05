package player.phonograph.glide;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;

import player.phonograph.glide.artistimage.ArtistImage;
import player.phonograph.glide.artistimage.ArtistImageLoader;
import player.phonograph.glide.audiocover.AudioFileCover;
import player.phonograph.glide.audiocover.AudioFileCoverLoader;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

@GlideModule
public class PhonographGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {

    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {

        registry.register(AudioFileCover.class, InputStream.class, new AudioFileCoverLoader.Factory());
        registry.register(ArtistImage.class, InputStream.class, new ArtistImageLoader.Factory());

        super.registerComponents(context, glide, registry);
    }
}
