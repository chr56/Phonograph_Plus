package player.phonograph.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.TransitionOptions;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.MediaStoreSignature;

import java.io.File;

import player.phonograph.R;
import player.phonograph.glide.palette.BitmapPaletteTranscoder;
import player.phonograph.glide.palette.BitmapPaletteWrapper;
import player.phonograph.model.Song;
import player.phonograph.util.MusicUtil;
import player.phonograph.util.PreferenceUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SongGlideRequest {

    public static final DiskCacheStrategy DEFAULT_DISK_CACHE_STRATEGY = DiskCacheStrategy.NONE;
    public static final int DEFAULT_ERROR_IMAGE = R.drawable.default_album_art;
    public static final int DEFAULT_ANIMATION = android.R.anim.fade_in;

    public static final RequestOptions DEFAULT_OPTION =
            new RequestOptions().diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
                    .error(DEFAULT_ERROR_IMAGE);

    public static final DrawableTransitionOptions DEFAULT_DRAWABLE_TRANSITION_OPTIONS = new DrawableTransitionOptions().transition(DEFAULT_ANIMATION);
    public static final BitmapTransitionOptions DEFAULT_BITMAP_TRANSITION_OPTIONS = new BitmapTransitionOptions().transition(DEFAULT_ANIMATION);

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
                    .apply(DEFAULT_OPTION)
                    .transition(DEFAULT_DRAWABLE_TRANSITION_OPTIONS)
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
                    .apply(DEFAULT_OPTION)
                    .transition(DEFAULT_BITMAP_TRANSITION_OPTIONS)
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

        public RequestBuilder<Bitmap> build() {
            return createBaseRequest(Bitmap.class, builder.requestManager, builder.song, builder.ignoreMediaStore)
                    .apply(DEFAULT_OPTION)
                    .transition(DEFAULT_BITMAP_TRANSITION_OPTIONS)
                    .transcode(new BitmapPaletteTranscoder(context), BitmapPaletteWrapper.class) //todo
                    .signature(createSignature(builder.song));
        }
    }

    public static <T> RequestBuilder<T> createBaseRequest(Class<T> type, RequestManager requestManager, Song song, boolean ignoreMediaStore) {
        if (ignoreMediaStore) {
            return requestManager.as(type).load(new File(song.data));
        } else {
            return requestManager.as(type).load(MusicUtil.getMediaStoreAlbumCoverUri(song.albumId));
        }
    }

    public static Key createSignature(Song song) {
        return new MediaStoreSignature("", song.dateModified, 0);
    }
}
