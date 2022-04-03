package player.phonograph.glide.audiocover;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import player.phonograph.BuildConfig;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class AudioFileCoverFetcher implements DataFetcher<InputStream> {
    private final AudioFileCover model;

    private InputStream stream;

    public AudioFileCoverFetcher(AudioFileCover model) {
        this.model = model;
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {

        final MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        byte[] picture = null;
        try {
            retriever.setDataSource(model.filePath);
            picture = retriever.getEmbeddedPicture();
        } catch (Exception ignored) {
        }

        if (picture != null) {
            stream = new ByteArrayInputStream(picture);
            callback.onDataReady(stream);
            return;
        } else {
            if (BuildConfig.DEBUG) Log.v(TAG, "No cover for " + model + " in MediaStore");
        }

        // use fallback
        try {
            stream = AudioFileCoverUtils.fallback(model.filePath);
            callback.onDataReady(stream);
            return;
        } catch (FileNotFoundException e) {
            if (BuildConfig.DEBUG) Log.v(TAG, "No cover for" + model + "in File");
        }


        // so onLoadFailed
        callback.onLoadFailed(new Exception("No Available Cover Picture For " + model));

    }

    @Override
    public void cleanup() {
        // already cleaned up in loadData and ByteArrayInputStream will be GC'd
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignore) {
                // can't do much about it
            }
        }
    }

    @Override
    public void cancel() {
        // cannot cancel
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

    static final String TAG = "AudioFileCoverFetcher";
}
