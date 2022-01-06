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

    private final ArtistImage model;

    private InputStream stream;

    private boolean ignoreMediaStore;

    public ArtistImageFetcher(final ArtistImage model, boolean ignoreMediaStore) {
        this.model = model;
        this.ignoreMediaStore = ignoreMediaStore;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        if (BuildConfig.DEBUG)
            Log.d("MOSAIC", "load data for " + model.artistName);

        try {
            stream = getMosaic(model.albumCovers);
            callback.onDataReady(stream);
            return;
        } catch (FileNotFoundException e) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "No Available Photo for " + model.artistName);
        }

        callback.onLoadFailed(new Exception("No Available Photo For " + model.artistName));

    }

    private InputStream getMosaic(final List<AlbumCover> albumCovers) throws FileNotFoundException {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        int artistBitMapSize = 512;

        final Map<InputStream, Integer> images = new HashMap<>();

        InputStream result = null;
        List<InputStream> streams = new ArrayList<>();

        try {
            for (final AlbumCover cover : albumCovers) {
                byte[] picture = null;
                if (!ignoreMediaStore) {
                    retriever.setDataSource(cover.getFilePath());
                    picture = retriever.getEmbeddedPicture();
                }
                final InputStream stream;
                if (picture != null) {
                    stream = new ByteArrayInputStream(picture);
                } else {
                    stream = AudioFileCoverUtils.fallback(cover.getFilePath());
                }

                if (stream != null) {
                    images.put(stream, cover.getYear());
                }
            }

            int nbImages = images.size();

            if (nbImages > 3) {
                streams = new ArrayList<>(images.keySet());

                int divisor = 1;
                for (int i = 1; i < nbImages && Math.pow(i, 2) <= nbImages; ++i) {
                    divisor = i;
                }
                divisor += 1;
                double nbTiles = Math.pow(divisor, 2);

                if (nbImages < nbTiles) {
                    divisor -= 1;
                    nbTiles = Math.pow(divisor, 2);
                }
                final int resize = (artistBitMapSize / divisor) + 1;

                final Bitmap bitmap = Bitmap.createBitmap(artistBitMapSize, artistBitMapSize, Bitmap.Config.RGB_565);
                final Canvas canvas = new Canvas(bitmap);

                int x = 0;
                int y = 0;

                for (int i = 0; i < streams.size() && i < nbTiles; ++i) {
                    final Bitmap bitmap1 = ImageUtil.resize(streams.get(i), resize, resize);
                    canvas.drawBitmap(bitmap1, x, y, null);
                    x += resize;

                    if (x >= artistBitMapSize) {
                        x = 0;
                        y += resize;
                    }
                }

                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
                result = new ByteArrayInputStream(bos.toByteArray());

            } else if (nbImages > 0) {
                // we return the last cover album of the artist
                Map.Entry<InputStream, Integer> maxEntryYear = null;

                for (final Map.Entry<InputStream, Integer> entry : images.entrySet()) {
                    if (maxEntryYear == null || entry.getValue()
                            .compareTo(maxEntryYear.getValue()) > 0) {
                        maxEntryYear = entry;
                    }
                }

                if (maxEntryYear != null) {
                    result = maxEntryYear.getKey();
                } else {
                    result = images.entrySet()
                            .iterator()
                            .next()
                            .getKey();
                }

            }
        } finally {
            retriever.release();
            try {
                for (final InputStream stream : streams) {
                    stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return result;
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

    private static final String TAG = "ArtistImageFetcher";

}
