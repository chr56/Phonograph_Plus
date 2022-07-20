package player.phonograph.glide.audiocover;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.InputStream;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class AudioFileCoverFetcher implements DataFetcher<InputStream> {

    private final AudioFileCoverFetchLogics imp;

    public AudioFileCoverFetcher(AudioFileCover model) {
        this.imp = new AudioFileCoverFetchLogics(model);
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
