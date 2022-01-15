package player.phonograph.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import player.phonograph.helper.menu.SongsMenuHelper;
import player.phonograph.interfaces.Displayable;
import player.phonograph.util.MusicUtil;
import player.phonograph.util.NavigationUtil;

public class Genre implements Parcelable, Displayable {
    public final long id;
    public final String name;
    public final int songCount;

    public Genre(final long id, final String name, final int songCount) {
        this.id = id;
        this.name = name;
        this.songCount = songCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Genre genre = (Genre) o;

        if (id != genre.id) return false;
        if (!name.equals(genre.name)) return false;
        return songCount == genre.songCount;
    }

    @Override
    public int hashCode() {
        long result = id;
        result = 31 * result + name.hashCode();
        result = 31 * result + songCount;
        return (int) result;
    }

    @Override
    public String toString() {
        return "Genre{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", songCount=" + songCount + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.name);
        dest.writeInt(this.songCount);
    }

    protected Genre(Parcel in) {
        this.id = in.readLong();
        this.name = in.readString();
        this.songCount = in.readInt();
    }

    @Keep
    public static final Creator<Genre> CREATOR = new Creator<Genre>() {
        public Genre createFromParcel(Parcel source) {
            return new Genre(source);
        }

        public Genre[] newArray(int size) {
            return new Genre[size];
        }
    };

    @Override
    public long getItemID() {
        return id;
    }

    @NonNull
    @Override
    public CharSequence getDisplayTitle() {
        return name;
    }

    @Nullable
    @Override
    public CharSequence getDescription() {
        return String.valueOf(songCount);
    }

    @Nullable
    @Override
    public Uri getPic() {
        return null;
    }

    @Nullable
    @Override
    public String getSortOrderReference() {
        return name;
    }

    @Override
    public int menuRes() {
        return 0;
    }

    @Nullable
    @Override
    public Function3<AppCompatActivity, Displayable, Integer, Boolean> menuHandler() {
        return null;
    }

    @Nullable
    @Override
    public Function3<AppCompatActivity, List<? extends Displayable>, Integer, Boolean> multiMenuHandler() {
        return (appCompatActivity, list, integer) -> {
            SongsMenuHelper.handleMenuClick(appCompatActivity, MusicUtil.getGenreSongList((List<Genre>) list),integer);
            return true;
        };
    }

    @NonNull
    @Override
    public Function3<FragmentActivity, Displayable, List<? extends Displayable>, Unit> clickHandler() {
        return (fragmentActivity, displayable, list) -> {
            NavigationUtil.goToGenre(fragmentActivity, (Genre) displayable);
            return null;
        };
    }
}
