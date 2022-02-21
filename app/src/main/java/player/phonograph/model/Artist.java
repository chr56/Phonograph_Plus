package player.phonograph.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;

import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import player.phonograph.App;
import player.phonograph.helper.menu.SongsMenuHelper;
import player.phonograph.interfaces.Displayable;
import player.phonograph.util.MusicUtil;
import player.phonograph.util.NavigationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class Artist implements Parcelable, Displayable {
    public static final String UNKNOWN_ARTIST_DISPLAY_NAME = "Unknown Artist";

    public final List<Album> albums;

    public Artist(List<Album> albums) {
        this.albums = albums;
    }

    public Artist() {
        this.albums = new ArrayList<>();
    }

    public long getId() {
        return safeGetFirstAlbum().getArtistId();
    }

    public String getName() {
        String name = safeGetFirstAlbum().getArtistName();
        if (MusicUtil.isArtistNameUnknown(name)) {
            return UNKNOWN_ARTIST_DISPLAY_NAME;
        }
        return name;
    }

    public int getSongCount() {
        int songCount = 0;
        for (Album album : albums) {
            songCount += album.getSongCount();
        }
        return songCount;
    }

    public int getAlbumCount() {
        return albums.size();
    }

    public List<Song> getSongs() {
        List<Song> songs = new ArrayList<>();
        for (Album album : albums) {
            songs.addAll(album.songs);
        }
        return songs;
    }

    @NonNull
    public Album safeGetFirstAlbum() {
        return albums.isEmpty() ? new Album() : albums.get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Artist artist = (Artist) o;

        return albums != null ? albums.equals(artist.albums) : artist.albums == null;

    }

    @Override
    public int hashCode() {
        return albums != null ? albums.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Artist{" +
                "albums=" + albums +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(this.albums);
    }

    protected Artist(Parcel in) {
        this.albums = in.createTypedArrayList(Album.CREATOR);
    }

    @Keep
    public static final Parcelable.Creator<Artist> CREATOR = new Parcelable.Creator<Artist>() {
        @Override
        public Artist createFromParcel(Parcel source) {
            return new Artist(source);
        }

        @Override
        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };

    @Override
    public long getItemID() {
        return getId();
    }

    @NonNull
    @Override
    public CharSequence getDisplayTitle() {
        return getName();
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return MusicUtil.getArtistInfoString(App.getInstance(), this);
    }

    @Nullable
    @Override
    public Uri getPic() {
        return null;//todo
    }

    @Nullable
    @Override
    public String getSortOrderReference() {
        return getName();//todo
    }

    @Override
    public int menuRes() {
        return 0;//todo
    }

    @Nullable
    @Override
    public Function3<AppCompatActivity, Displayable, Integer, Boolean> menuHandler() {
        return null;//todo artist menu action
    }

    @Nullable
    @Override
    public Function3<AppCompatActivity, List<? extends Displayable>, Integer, Boolean> multiMenuHandler() {
        return (appCompatActivity, list, integer) -> SongsMenuHelper.handleMenuClick(appCompatActivity, MusicUtil.getArtistSongList((List<Artist>) list), integer);//todo more variety
    }

    @NonNull
    @Override
    public Function3<FragmentActivity, Displayable, List<? extends Displayable>, Unit> clickHandler() {
        return (fragmentActivity, displayable, queue) -> {
            NavigationUtil.goToArtist(fragmentActivity, ((Artist) displayable).getId(), (Pair[]) null);//todo animate
            return null;
        };
    }
}
