package player.phonograph.glide;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

import java.io.InputStream;

import player.phonograph.glide.artistimage.ArtistImage;
import player.phonograph.glide.artistimage.ArtistImageLoader;
import player.phonograph.glide.audiocover.AudioFileCover;
import player.phonograph.glide.audiocover.AudioFileCoverLoader;
import player.phonograph.glide.palette.BitmapPaletteTranscoder;
import player.phonograph.glide.palette.BitmapPaletteWrapper;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

@GlideModule
public class PhonographGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setDefaultRequestOptions(new RequestOptions().format(DecodeFormat.PREFER_RGB_565));
    }
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {

        registry.prepend(AudioFileCover.class, InputStream.class, new AudioFileCoverLoader.Factory());
        registry.prepend(ArtistImage.class, InputStream.class, new ArtistImageLoader.Factory());

        registry.register(Bitmap.class, BitmapPaletteWrapper.class, new BitmapPaletteTranscoder(glide.getBitmapPool()));

        super.registerComponents(context, glide, registry);
    }
}
