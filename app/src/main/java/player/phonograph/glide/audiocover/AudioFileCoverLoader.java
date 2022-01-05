package player.phonograph.glide.audiocover;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

public class AudioFileCoverLoader implements ModelLoader<AudioFileCover, InputStream> {

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull AudioFileCover audioFileCover, int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(audioFileCover), new AudioFileCoverFetcher(audioFileCover));
    }

    @Override
    public boolean handles(@NonNull AudioFileCover audioFileCover) {
        return !audioFileCover.filePath.isEmpty();
    }


    public static class Factory implements ModelLoaderFactory<AudioFileCover, InputStream> {
        @NonNull
        @Override
        public ModelLoader<AudioFileCover, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new AudioFileCoverLoader();
        }

        @Override
        public void teardown() {
        }
    }
}

