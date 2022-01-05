package player.phonograph.glide.palette;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

public class BitmapPaletteWrapper {
    @NonNull
    private final Bitmap mBitmap;
    private final Palette mPalette;

    public BitmapPaletteWrapper(@NonNull Bitmap bitmap, Palette palette) {
        mBitmap = bitmap;
        mPalette = palette;
    }

    @NonNull
    public Bitmap getBitmap() {
        return mBitmap;
    }

    @Nullable
    public Palette getPalette() {
        return mPalette;
    }
}
