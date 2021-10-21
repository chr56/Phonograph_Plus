package player.phonograph.glide;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.request.animation.GlideAnimation;
import player.phonograph.R;
import player.phonograph.glide.palette.BitmapPaletteTarget;
import player.phonograph.glide.palette.BitmapPaletteWrapper;
import player.phonograph.util.PhonographColorUtil;

import chr_56.MDthemer.util.Util;

public abstract class PhonographColoredTarget extends BitmapPaletteTarget {
    public PhonographColoredTarget(ImageView view) {
        super(view);
    }

    @Override
    public void onLoadFailed(Exception e, Drawable errorDrawable) {
        super.onLoadFailed(e, errorDrawable);
        onColorReady(getDefaultFooterColor());
    }

    @Override
    public void onResourceReady(BitmapPaletteWrapper resource, GlideAnimation<? super BitmapPaletteWrapper> glideAnimation) {
        super.onResourceReady(resource, glideAnimation);
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
