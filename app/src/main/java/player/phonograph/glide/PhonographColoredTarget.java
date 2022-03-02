package player.phonograph.glide;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.transition.Transition;

import util.mddesign.util.Util;
import player.phonograph.R;
import player.phonograph.glide.palette.BitmapPaletteTarget;
import player.phonograph.glide.palette.BitmapPaletteWrapper;
import player.phonograph.util.PhonographColorUtil;

public abstract class PhonographColoredTarget extends BitmapPaletteTarget {
    public PhonographColoredTarget(ImageView view) {
        super(view);
    }

    @Override
    public void onLoadFailed(Drawable errorDrawable) {
        super.onLoadFailed(errorDrawable);
        onColorReady(getDefaultFooterColor());
    }

    @Override
    public void onResourceReady(@NonNull BitmapPaletteWrapper resource, @Nullable Transition<? super BitmapPaletteWrapper> transition) {
        super.onResourceReady(resource, transition);
        onColorReady(PhonographColorUtil.getColor(resource.getPalette(), getDefaultFooterColor()));
    }

    protected int getDefaultFooterColor() {
        return Util.resolveColor(getView().getContext(), R.attr.defaultFooterColor);
    }

    protected int getAlbumArtistFooterColor() {
        return Util.resolveColor(getView().getContext(), R.attr.cardBackgroundColor);
    }

    public abstract void onColorReady(int color);
}
