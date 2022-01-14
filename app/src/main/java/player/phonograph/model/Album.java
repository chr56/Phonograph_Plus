package player.phonograph.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import player.phonograph.App;
import player.phonograph.R;
import player.phonograph.helper.menu.SongsMenuHelper;
import player.phonograph.interfaces.Displayable;
import player.phonograph.util.MusicUtil;
import player.phonograph.util.NavigationUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class Album implements Parcelable, Displayable {
    public final List<Song> songs;

    public Album(List<Song> songs) {
        this.songs = songs;
    }

    public Album() {
        this.songs = new ArrayList<>();
    }

    public long getId() {
        return safeGetFirstSong().albumId;
    }

    public String getTitle() {
        return safeGetFirstSong().albumName;
    }

    public long getArtistId() {
        return safeGetFirstSong().artistId;
    }

    public String getArtistName() {
        return safeGetFirstSong().artistName;
    }

    public int getYear() {
        return safeGetFirstSong().year;
    }

    public long getDateModified() {
        return safeGetFirstSong().dateModified;
    }

    public int getSongCount() {
        return songs.size();
    }

    @NonNull
    public Song safeGetFirstSong() {
        return songs.isEmpty() ? Song.EMPTY_SONG : songs.get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Album that = (Album) o;

        return songs != null ? songs.equals(that.songs) : that.songs == null;

    }

    @Override
    public int hashCode() {
        return songs != null ? songs.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Album{" +
                "songs=" + songs +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(songs);
    }

    protected Album(Parcel in) {
        this.songs = in.createTypedArrayList(Song.CREATOR);
    }

    @Keep
    public static final Creator<Album> CREATOR = new Creator<Album>() {
        public Album createFromParcel(Parcel source) {
            return new Album(source);
        }

        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    @Override
    public long getItemID() {
        return getArtistId();
    }

    @NonNull
    @Override
    public CharSequence getDisplayTitle() {
        return getTitle();
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return MusicUtil.buildInfoString(
                getArtistName(),
                MusicUtil.getSongCountString(App.getInstance(), songs.size())
        );
    }

    @Nullable
    @Override
    public Uri getPic() {
        return null;//todo
    }

    @Nullable
    @Override
    public String getSortOrderReference() {
        return getTitle();//todo
    }

    @Override
    public int menuRes() {
        return 0;//todo
    }

    @Nullable
    @Override
    public Function3<AppCompatActivity, Displayable, Integer, Boolean> menuHandler() {
        return null;//todo album menu action
    }

    @Nullable
    @Override
    public Function3<AppCompatActivity, List<? extends Displayable>, Integer, Boolean> multiMenuHandler() {
        return (appCompatActivity, list, integer) -> SongsMenuHelper.handleMenuClick(appCompatActivity, MusicUtil.getSongList((List<Album>) list), integer);//todo more variety
    }

    @NonNull
    @Override
    public Function3<FragmentActivity, Displayable, List<? extends Displayable>, Unit> clickHandler() {
        return (fragmentActivity, displayable, queue) -> {
            NavigationUtil.goToAlbum(fragmentActivity, ((Album) displayable).getId(), (Pair[]) null);//todo animate
            return null;
        };
    }
}
