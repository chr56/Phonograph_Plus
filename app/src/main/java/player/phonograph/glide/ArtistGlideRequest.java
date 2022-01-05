package player.phonograph.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;

import player.phonograph.App;
import player.phonograph.R;
import player.phonograph.glide.artistimage.AlbumCover;
import player.phonograph.glide.artistimage.ArtistImage;
import player.phonograph.glide.palette.BitmapPaletteWrapper;
import player.phonograph.model.Album;
import player.phonograph.model.Artist;
import player.phonograph.model.Song;
import player.phonograph.util.ArtistSignatureUtil;
import player.phonograph.util.CustomArtistImageUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class ArtistGlideRequest {

    private static final DiskCacheStrategy DEFAULT_DISK_CACHE_STRATEGY = DiskCacheStrategy.ALL;
    private static final int DEFAULT_ERROR_IMAGE = R.drawable.default_artist_image;
    public static final int DEFAULT_ANIMATION = android.R.anim.fade_in;

    public static final RequestOptions DEFAULT_OPTION =
            new RequestOptions().diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
                    .priority(Priority.LOW)
                    .error(DEFAULT_ERROR_IMAGE);

    public static final DrawableTransitionOptions DEFAULT_DRAWABLE_TRANSITION_OPTIONS = new DrawableTransitionOptions().transition(DEFAULT_ANIMATION);
    public static final BitmapTransitionOptions DEFAULT_BITMAP_TRANSITION_OPTIONS = new BitmapTransitionOptions().transition(DEFAULT_ANIMATION);


    public static class Builder {
        final RequestManager requestManager;
        final Artist artist;
        boolean noCustomImage;

        public static Builder from(@NonNull RequestManager requestManager, Artist artist) {
            return new Builder(requestManager, artist);
        }

        private Builder(@NonNull RequestManager requestManager, Artist artist) {
            this.requestManager = requestManager;
            this.artist = artist;
        }

        public PaletteBuilder generatePalette(Context context) {
            return new PaletteBuilder(this, context);
        }

        public BitmapBuilder asBitmap() {
            return new BitmapBuilder(this);
        }

        public Builder noCustomImage(boolean noCustomImage) {
            this.noCustomImage = noCustomImage;
            return this;
        }

        public RequestBuilder<Drawable> build() {
            return createBaseRequest(Drawable.class, requestManager, artist, noCustomImage)
                    .apply(DEFAULT_OPTION)
                    .transition(DEFAULT_DRAWABLE_TRANSITION_OPTIONS)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .signature(createSignature(artist));
        }
    }

    public static class BitmapBuilder {
        private final Builder builder;

        public BitmapBuilder(Builder builder) {
            this.builder = builder;
        }

        public RequestBuilder<Bitmap> build() {
            return createBaseRequest(Bitmap.class, builder.requestManager, builder.artist, builder.noCustomImage)
                    .apply(DEFAULT_OPTION)
                    .transition(DEFAULT_BITMAP_TRANSITION_OPTIONS)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .signature(createSignature(builder.artist));
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
            return createBaseRequest(BitmapPaletteWrapper.class, builder.requestManager, builder.artist, builder.noCustomImage)
                    .apply(DEFAULT_OPTION)
//                    .transition(DEFAULT_BITMAP_TRANSITION_OPTIONS)//todo
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .signature(createSignature(builder.artist));
        }
    }

    public static <T> RequestBuilder<T> createBaseRequest(Class<T> type, RequestManager requestManager, Artist artist, boolean noCustomImage) {
        boolean hasCustomImage = CustomArtistImageUtil.getInstance(App.getInstance()).hasCustomArtistImage(artist);

        if (noCustomImage || !hasCustomImage) {
            final List<AlbumCover> songs = new ArrayList<>();
            for (final Album album : artist.albums) {
                final Song song = album.safeGetFirstSong();
                songs.add(new AlbumCover(album.getYear(), song.data));
            }

            return requestManager.as(type).load(new ArtistImage(artist.getName(), songs)); //todo
        } else {
            return requestManager.as(type).load(CustomArtistImageUtil.getFile(artist));
        }
    }

    private static Key createSignature(Artist artist) {
        return ArtistSignatureUtil.getInstance(App.getInstance()).getArtistSignature(artist.getName());
    }
}
