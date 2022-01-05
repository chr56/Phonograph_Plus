package player.phonograph.glide.palette;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.palette.graphics.Palette;

public class BitmapPaletteWrapper {
    @NonNull
    private final Bitmap mBitmap;
    @NonNull
    private final Palette mPalette;

    public BitmapPaletteWrapper(@NonNull Bitmap bitmap, @NonNull Palette palette) {
        mBitmap = bitmap;
        mPalette = palette;
    }

    @NonNull
    public Bitmap getBitmap() {
        return mBitmap;
    }

    @NonNull
    public Palette getPalette() {
        return mPalette;
    }
}
