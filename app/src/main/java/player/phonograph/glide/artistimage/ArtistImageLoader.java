package player.phonograph.glide.artistimage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

import player.phonograph.App;
import player.phonograph.util.PreferenceUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

public class ArtistImageLoader implements ModelLoader<ArtistImage, InputStream> {

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull ArtistImage artistImage, int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(artistImage), new ArtistImageFetcher(artistImage, PreferenceUtil.getInstance(App.getInstance()).ignoreMediaStoreArtwork()));
    }

    @Override
    public boolean handles(@NonNull ArtistImage artistImage) {
        return !artistImage.albumCovers.isEmpty();
    }

    public static class Factory implements ModelLoaderFactory<ArtistImage, InputStream> {

        @NonNull
        @Override
        public ModelLoader<ArtistImage, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new ArtistImageLoader();
        }

        @Override
        public void teardown() {

        }
    }
}

