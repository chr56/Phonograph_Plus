package player.phonograph.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import player.phonograph.database.mediastore.ArtistWithAlbums;
import player.phonograph.database.mediastore.ArtistWithSongs;
import player.phonograph.database.mediastore.Converter;
import player.phonograph.database.mediastore.MusicDatabase;
import player.phonograph.util.MusicUtil;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
// fixme remove hacker trick & migrate to new room database
public class Artist implements Parcelable {
    public static final String UNKNOWN_ARTIST_DISPLAY_NAME = "Unknown Artist";

    public final List<Album> albums;

    public Artist(List<Album> albums) {
        this.albums = albums;
    }

    public Artist() {
        this.albums = new ArrayList<>();
    }

    private long id = 0;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        if (id != 0) return id;


        return safeGetFirstAlbum().getArtistId();
    }

    private String name = null;

    public void setName(String s) {
        this.name = s;
    }

    public String getName() {
        if (name != null) return name;
        String name = safeGetFirstAlbum().getArtistName();
        if (MusicUtil.isArtistNameUnknown(name)) {
            return UNKNOWN_ARTIST_DISPLAY_NAME;
        }
        return name;
    }

    public int getSongCount() {
        int songCount = 0;
        if (albums != null)
            for (Album album : albums) {
                songCount += album.getSongCount();
            }
        return songCount;
    }

    public int getAlbumCount() {
        if (albums != null)
            return albums.size();
        else return 0;
    }


    public List<Song> getSongs() {
        ArtistWithSongs t = MusicDatabase.INSTANCE.getSongsDataBase().ArtistSongsDao().getArtistSong(getId());
        if (t != null) return
                Converter.convertSong(
                        t.getSongs()

                );
        else return new ArrayList<Song>();


//        List<Song> songs = new ArrayList<>();
//        for (Album album : albums) {
//            songs.addAll(album.songs);
//        }
//        return songs;
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
}
