package player.phonograph.glide.palette;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.util.Util;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class BitmapPaletteResource implements Resource<BitmapPaletteWrapper> {

    private final BitmapPaletteWrapper bitmapPaletteWrapper;
    private final BitmapPool bitmapPool;

    public BitmapPaletteResource(BitmapPaletteWrapper bitmapPaletteWrapper, BitmapPool bitmapPool) {
        this.bitmapPaletteWrapper = bitmapPaletteWrapper;
        this.bitmapPool = bitmapPool;
    }

    @NonNull
    @Override
    public Class<BitmapPaletteWrapper> getResourceClass() {
        return BitmapPaletteWrapper.class;
    }

    @NonNull
    @Override
    public BitmapPaletteWrapper get() {
        return bitmapPaletteWrapper;
    }

    @Override
    public int getSize() {
        return Util.getBitmapByteSize(bitmapPaletteWrapper.getBitmap());
    }

    @Override
    public void recycle() {
        //todo check
        Bitmap bm = bitmapPaletteWrapper.getBitmap();

        if (!bm.isRecycled()) {
            try {
                bm.recycle();
            } catch (Exception e) {
                Log.e("BitmapPaletteResource", "BitmapPalette can not be recycled");
                Log.w("BitmapPaletteResource", e.getMessage());
            }
        }

    }
}
