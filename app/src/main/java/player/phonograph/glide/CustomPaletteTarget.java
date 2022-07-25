package player.phonograph.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import player.phonograph.R;
import player.phonograph.glide.palette.BitmapPaletteWrapper;
import player.phonograph.util.PhonographColorUtil;
import util.mddesign.util.Util;

/**
 * A target that merely aiming at fetch Bitmap with its Palette color only.
 */
public abstract class CustomPaletteTarget extends CustomTarget<BitmapPaletteWrapper> {

    private final Context context;

    public CustomPaletteTarget(Context context) {
        super();
        this.context = context;
    }


    @Override
    public void onLoadFailed(Drawable errorDrawable) {
        onColorReady(getDefaultFooterColor());
    }

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
        onColorReady(getDefaultFooterColor());
    }

    @Override
    public void onResourceReady(@NonNull BitmapPaletteWrapper resource, @Nullable Transition<? super BitmapPaletteWrapper> transition) {
        onResourceReady(resource);
        onColorReady(PhonographColorUtil.getColor(resource.getPalette(), getDefaultFooterColor()));
    }

    protected int getDefaultFooterColor() {
        return Util.resolveColor(context, R.attr.defaultFooterColor);
    }

    protected int getAlbumArtistFooterColor() {
        return Util.resolveColor(context, R.attr.cardBackgroundColor);
    }

    public abstract void onResourceReady(@NonNull BitmapPaletteWrapper resource);

    public abstract void onColorReady(int color);
}
