/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package util.phonograph.tageditor;

import static mt.tint.ActivityColor.setNavigationBarColor;
import static mt.tint.ActivityColor.setTaskDescriptionColor;
import static mt.util.color.ColorUtil.withAlpha;
import static mt.util.color.ToolbarColor.toolbarTitleColor;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.WhichButton;
import com.afollestad.materialdialogs.actions.DialogActionExtKt;
import com.afollestad.materialdialogs.list.DialogListExtKt;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jaudiotagger.tag.FieldKey;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import kotlin.Unit;
import lib.phonograph.activity.ToolbarActivity;
import mt.pref.ThemeColor;
import mt.tint.ActivityColor;
import mt.tint.viewtint.Auto;
import player.phonograph.R;
import player.phonograph.misc.SimpleObservableScrollViewCallbacks;
import player.phonograph.util.Util;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public abstract class AbsTagEditorActivity extends ToolbarActivity {

    public static final String EXTRA_ID = "extra_id";
    public static final String EXTRA_PALETTE = "extra_palette";
    private static final int REQUEST_CODE_SELECT_IMAGE = 1000;

    FloatingActionButton fab;
    ObservableScrollView observableScrollView;
    Toolbar toolbar;
    ImageView image;
    LinearLayout header;
    TagEditorViewModel model;

    private int headerVariableSpace;
    private int paletteColorPrimary;
    private boolean isInNoImageMode;
    private final SimpleObservableScrollViewCallbacks observableScrollViewCallbacks = new SimpleObservableScrollViewCallbacks() {
        @Override
        public void onScrollChanged(int scrollY, boolean b, boolean b2) {
            float alpha;
            if (!isInNoImageMode) {
                alpha = 1 - (float) Math.max(0, headerVariableSpace - scrollY) / headerVariableSpace;
            } else {
                header.setTranslationY(scrollY);
                alpha = 1;
            }
            toolbar.setBackgroundColor(withAlpha(paletteColorPrimary, alpha));
            image.setTranslationY(scrollY / 2);
        }
    };

    protected AbsTagEditorActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        model = new ViewModelProvider(this).get(TagEditorViewModel.class);

        setAutoSetStatusBarColor(false);
        setAutoSetNavigationBarColor(false);
        setAutoSetTaskDescriptionColor(false);
        super.onCreate(savedInstanceState);
        setContentView(getContentViewLayout());

        fab = findViewById(R.id.play_pause_fab);
        observableScrollView = findViewById(R.id.observableScrollView);
        toolbar = findViewById(R.id.toolbar);
        image = findViewById(R.id.image);
        header = findViewById(R.id.header);

        getIntentExtras();

        model.setSongPaths(
                getSongPaths()
        );
        if (model.getSongPaths() != null && model.getSongPaths().isEmpty()) {
            finish();
            return;
        }

        headerVariableSpace = getResources().getDimensionPixelSize(R.dimen.tagEditorHeaderVariableSpace);

        setUpViews();

        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ActivityColor.setActivityToolbarColorAuto(this, toolbar);
    }

    private void setUpViews() {
        setUpScrollView();
        setUpFab();
        setUpImageView();
    }

    private void setUpScrollView() {
        observableScrollView.setScrollViewCallbacks(observableScrollViewCallbacks);
    }

    @SuppressLint("CheckResult")
    private void setUpImageView() {
        loadCurrentImage();
        final String[] items = new String[]{
                getString(R.string.download_from_last_fm),
                getString(R.string.pick_from_local_storage),
                getString(R.string.web_search),
                getString(R.string.remove_cover)
        };
        image.setOnClickListener(v -> {
            MaterialDialog dialog = new MaterialDialog(AbsTagEditorActivity.this, MaterialDialog.getDEFAULT_BEHAVIOR())
                    .title(R.string.update_image, null);
            DialogListExtKt.listItems(dialog, null, Arrays.asList(items), null, true,
                    this::invoke
            );
            //set button color
            DialogActionExtKt.getActionButton(dialog, WhichButton.POSITIVE).updateTextColor(ThemeColor.INSTANCE.accentColor(this));
            dialog.show();
        });
    }

    private void startImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.pick_from_local_storage)), REQUEST_CODE_SELECT_IMAGE);
    }

    protected abstract void loadCurrentImage();

    protected abstract void getImageFromLastFM();

    protected abstract void searchImageOnWeb();

    protected abstract void deleteImage();

    private void setUpFab() {
        fab.setScaleX(0);
        fab.setScaleY(0);
        fab.setEnabled(false);
        fab.setOnClickListener(v -> save());

        Auto.setTintAuto(fab, ThemeColor.INSTANCE.accentColor(this), true);
    }

    protected abstract void save();

    private void getIntentExtras() {
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            model.setId(intentExtras.getLong(EXTRA_ID));
        }
    }

    protected abstract View getContentViewLayout();

    @NonNull
    protected abstract List<String> getSongPaths();

    protected void searchWebFor(String... keys) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String key : keys) {
            stringBuilder.append(key);
            stringBuilder.append(" ");
        }
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, stringBuilder.toString());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setNoImageMode() {
        isInNoImageMode = true;
        image.setVisibility(View.GONE);
        image.setEnabled(false);
        observableScrollView.setPadding(0, Util.getActionBarSize(this), 0, 0);
        observableScrollViewCallbacks.onScrollChanged(observableScrollView.getCurrentScrollY(), false, false);

        setColors(getIntent().getIntExtra(EXTRA_PALETTE, ThemeColor.INSTANCE.primaryColor(this)));
        toolbar.setBackgroundColor(paletteColorPrimary);
    }

    protected void dataChanged() {
        showFab();
    }

    private void showFab() {
        fab.animate()
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator())
                .scaleX(1)
                .scaleY(1)
                .start();
        fab.setEnabled(true);
    }

    protected void setImageBitmap(@Nullable final Bitmap bitmap, int bgColor) {
        if (bitmap == null) {
            image.setImageResource(R.drawable.default_album_art);
        } else {
            image.setImageBitmap(bitmap);
        }
        setColors(bgColor);
    }

    protected void setColors(int color) {
        paletteColorPrimary = color;
        observableScrollViewCallbacks.onScrollChanged(observableScrollView.getCurrentScrollY(), false, false);
        header.setBackgroundColor(paletteColorPrimary);
        setStatusbarColor(paletteColorPrimary);
        setNavigationBarColor(this, paletteColorPrimary);
        setTaskDescriptionColor(this, paletteColorPrimary);

        toolbar.setTitleTextColor(toolbarTitleColor(this, color));
    }

    protected void writeValuesToFiles(@NonNull final Map<FieldKey, String> fieldKeyValueMap, @Nullable final ArtworkInfo artworkInfo) {
        Util.hideSoftKeyboard(this);
        model.writeTagsToSong(new LoadingInfo(getSongPaths(), fieldKeyValueMap, artworkInfo), this);
    }

    private Unit invoke(MaterialDialog dialog1, Integer index, CharSequence text) {
        switch (index) {
            case 0:
                getImageFromLastFM();
                break;
            case 1:
                startImagePicker();
                break;
            case 2:
                searchImageOnWeb();
                break;
            case 3:
                deleteImage();
                break;
        }
        return null;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                Uri selectedImage = imageReturnedIntent.getData();
                loadImageFromFile(selectedImage);
            }
        }
    }

    protected abstract void loadImageFromFile(Uri selectedFile);

}
