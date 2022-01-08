package player.phonograph.ui.activities.tageditor;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import org.jaudiotagger.tag.FieldKey;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import chr_56.MDthemer.util.ToolbarColorUtil;
import player.phonograph.R;
import player.phonograph.database.mediastore.MusicDatabase;
import player.phonograph.database.mediastore.SongConverter;
import player.phonograph.databinding.ActivitySongTagEditorBinding;
import player.phonograph.loader.SongLoader;

public class SongTagEditorActivity extends AbsTagEditorActivity implements TextWatcher {

    protected ActivitySongTagEditorBinding viewBinding;

    EditText songTitle;
    EditText albumTitle;
    EditText artist;
    EditText genre;
    EditText year;
    EditText trackNumber;
    EditText lyrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewBinding = ActivitySongTagEditorBinding.inflate(getLayoutInflater());
        bind();
        super.onCreate(savedInstanceState);

        setNoImageMode();
        setUpViews();

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(R.string.action_tag_editor);
    }

    private void bind() {
        songTitle = viewBinding.title1;
        albumTitle = viewBinding.title2;
        artist = viewBinding.artist;
        genre = viewBinding.genre;
        year = viewBinding.year;
        trackNumber = viewBinding.imageText;
        lyrics = viewBinding.lyrics;

    }

    private void setUpViews() {
        fillViewsWithFileTags();
        songTitle.addTextChangedListener(this);
        albumTitle.addTextChangedListener(this);
        artist.addTextChangedListener(this);
        genre.addTextChangedListener(this);
        year.addTextChangedListener(this);
        trackNumber.addTextChangedListener(this);
        lyrics.addTextChangedListener(this);
    }

    private void fillViewsWithFileTags() {
        songTitle.setText(getSongTitle());
        albumTitle.setText(getAlbumTitle());
        artist.setText(getArtistName());
        genre.setText(getGenreName());
        year.setText(getSongYear());
        trackNumber.setText(getTrackNumber());
        lyrics.setText(getLyrics());
    }

    @Override
    protected void loadCurrentImage() {

    }

    @Override
    protected void getImageFromLastFM() {

    }

    @Override
    protected void searchImageOnWeb() {

    }

    @Override
    protected void deleteImage() {

    }

    @Override
    protected void save() {
        Map<FieldKey, String> fieldKeyValueMap = new EnumMap<>(FieldKey.class);
        fieldKeyValueMap.put(FieldKey.TITLE, songTitle.getText().toString());
        fieldKeyValueMap.put(FieldKey.ALBUM, albumTitle.getText().toString());
        fieldKeyValueMap.put(FieldKey.ARTIST, artist.getText().toString());
        fieldKeyValueMap.put(FieldKey.GENRE, genre.getText().toString());
        fieldKeyValueMap.put(FieldKey.YEAR, year.getText().toString());
        fieldKeyValueMap.put(FieldKey.TRACK, trackNumber.getText().toString());
        fieldKeyValueMap.put(FieldKey.LYRICS, lyrics.getText().toString());
        writeValuesToFiles(fieldKeyValueMap, null);
    }

    @Override
    protected View getContentViewLayout() {
        return viewBinding.getRoot();
    }

    @NonNull
    @Override
    protected List<String> getSongPaths() {
        List<String> paths = new ArrayList<>(1);
        paths.add(
                MusicDatabase.INSTANCE.getSongsDataBase().SongDao().findSongById(getId()).get(0).getPath()
//                SongLoader.getSong(this, getId()).data
        );
        return paths;
    }

    @Override
    protected void loadImageFromFile(Uri imageFilePath) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        dataChanged();
    }

    @Override
    protected void setColors(int color) {
        super.setColors(color);
        int toolbarTitleColor = ToolbarColorUtil.toolbarTitleColor(this, color);
        songTitle.setTextColor(toolbarTitleColor);
        albumTitle.setTextColor(toolbarTitleColor);
    }
}
