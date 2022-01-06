package player.phonograph.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.signature.MediaStoreSignature;

import player.phonograph.glide.audiocover.AudioFileCover;
import player.phonograph.glide.palette.BitmapPaletteWrapper;
import player.phonograph.model.Song;
import player.phonograph.util.MusicUtil;
import player.phonograph.util.PreferenceUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SongGlideRequest {

    public static class Builder {
        final RequestManager requestManager;
        final Song song;
        boolean ignoreMediaStore;

        public static Builder from(@NonNull RequestManager requestManager, Song song) {
            return new Builder(requestManager, song);
        }

        private Builder(@NonNull RequestManager requestManager, Song song) {
            this.requestManager = requestManager;
            this.song = song;
        }

        public PaletteBuilder generatePalette(Context context) {
            return new PaletteBuilder(this, context);
        }

        public BitmapBuilder asBitmap() {
            return new BitmapBuilder(this);
        }

        public Builder checkIgnoreMediaStore(Context context) {
            return ignoreMediaStore(PreferenceUtil.getInstance(context).ignoreMediaStoreArtwork());
        }

        public Builder ignoreMediaStore(boolean ignoreMediaStore) {
            this.ignoreMediaStore = ignoreMediaStore;
            return this;
        }

        public RequestBuilder<Drawable> build() {
            return createBaseRequest(Drawable.class, requestManager, song, ignoreMediaStore)
                    .apply(GlideRequestOptions.get_default_option_song())
                    .transition(GlideRequestOptions.get_default_drawable_transition_options())
                    .signature(createSignature(song));
        }
    }

    public static class BitmapBuilder {
        private final Builder builder;

        public BitmapBuilder(Builder builder) {
            this.builder = builder;
        }

        public RequestBuilder<Bitmap> build() {
            return createBaseRequest(Bitmap.class, builder.requestManager, builder.song, builder.ignoreMediaStore)
                    .apply(GlideRequestOptions.get_default_option_song())
                    .transition(GlideRequestOptions.get_default_bitmap_transition_options())
                    .signature(createSignature(builder.song));
        }
    }

    public static class PaletteBuilder {
        final Context context;
        private final Builder builder;

        public PaletteBuilder(Builder builder, Context context) {
            this.builder = builder;
            this.context = context;
        }

        public RequestBuilder<BitmapPaletteWrapper> build() {
            return createBaseRequest(BitmapPaletteWrapper.class, builder.requestManager, builder.song, builder.ignoreMediaStore)
                    .apply(GlideRequestOptions.get_default_option_song())
//                    .transition(DEFAULT_BITMAP_TRANSITION_OPTIONS) //todo
                    .signature(createSignature(builder.song));
        }
    }

    public static <T> RequestBuilder<T> createBaseRequest(Class<T> type, RequestManager requestManager, Song song, boolean ignoreMediaStore) {
        if (ignoreMediaStore) {
            return requestManager.as(type).load(new AudioFileCover(song.data));
        } else {
            return requestManager.as(type).load(MusicUtil.getMediaStoreAlbumCoverUri(song.albumId));
        }
    }

    public static Key createSignature(Song song) {
        return new MediaStoreSignature("", song.dateModified, 0);
    }
}
