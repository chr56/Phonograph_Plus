package player.phonograph.glide.artistimage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import player.phonograph.BuildConfig;
import player.phonograph.glide.audiocover.AudioFileCoverUtils;
import player.phonograph.util.ImageUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class ArtistImageFetcher implements DataFetcher<InputStream> {

    private final ArtistImageFetchLogics imp;

    public ArtistImageFetcher(final ArtistImage model, boolean ignoreMediaStore) {
        this.imp = new ArtistImageFetchLogics(model, ignoreMediaStore);
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        imp.fetch(callback);
    }


    @Override
    public void cleanup() {
        imp.cleanup();
    }

    @Override
    public void cancel() {

    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }

}
