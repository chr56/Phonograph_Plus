package player.phonograph.interfaces;

import androidx.annotation.NonNull;

import com.afollestad.materialcab.MaterialCab;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public interface CabHolder {

    @NonNull
    MaterialCab showCab(final int menuRes, final MaterialCab.Callback callback);
}
