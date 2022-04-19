package player.phonograph.ui.fragments.mainactivity.folders;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialcab.MaterialCabKt;
import com.afollestad.materialcab.attached.AttachedCab;
import com.afollestad.materialcab.attached.AttachedCabKt;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.WhichButton;
import com.afollestad.materialdialogs.actions.DialogActionExtKt;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import player.phonograph.App;
import player.phonograph.R;
import player.phonograph.adapter.SongFileAdapter;
import player.phonograph.databinding.FragmentFolderBinding;
import player.phonograph.helper.menu.SongMenuHelper;
import player.phonograph.helper.menu.SongsMenuHelper;
import player.phonograph.interfaces.CabHolder;
import player.phonograph.misc.DialogAsyncTask;
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener;
import player.phonograph.model.Song;
import player.phonograph.service.MusicPlayerRemote;
import player.phonograph.settings.Setting;
import player.phonograph.ui.activities.MainActivity;
import player.phonograph.ui.fragments.mainactivity.AbsMainActivityFragment;
import player.phonograph.util.BlacklistUtil;
import player.phonograph.util.FileUtil;
import player.phonograph.util.PhonographColorUtil;
import player.phonograph.util.ViewUtil;
import player.phonograph.views.BreadCrumbLayout;
import util.mdcolor.ColorUtil;
import util.mdcolor.pref.ThemeColor;
import util.mddesign.util.MaterialColorHelper;
import util.mddesign.util.TintHelper;
import util.mddesign.util.ToolbarColorUtil;

@SuppressLint("NonConstantResourceId")
public class FoldersFragment extends AbsMainActivityFragment implements MainActivity.MainActivityFragmentCallbacks, CabHolder, BreadCrumbLayout.SelectionCallback, SongFileAdapter.Callbacks, AppBarLayout.OnOffsetChangedListener {

    protected static final String PATH = "path";
    protected static final String CRUMBS = "crumbs";

    private FragmentFolderBinding viewBinding;

    private SongFileAdapter adapter;
    private AttachedCab cab;

    private RecyclerView.AdapterDataObserver dataObserver;

    private FolderFragmentViewModel model;

    public FoldersFragment() {
    }

    public static FoldersFragment newInstance(Context context) {
        return newInstance(Setting.instance().getStartDirectory());
    }

    public static FoldersFragment newInstance(File directory) {
        FoldersFragment frag = new FoldersFragment();
        Bundle b = new Bundle();
        b.putSerializable(PATH, directory);
        frag.setArguments(b);
        return frag;
    }

    public void setCrumb(BreadCrumbLayout.Crumb crumb, boolean addToHistory) {
        if (crumb == null) return;
        saveScrollPosition();
        viewBinding.breadCrumbs.setActiveOrAdd(crumb, false);
        if (addToHistory) {
            viewBinding.breadCrumbs.addHistory(crumb);
        }
        model.loadFiles(crumb,files -> {
            updateAdapter((List<File>) files);
            return Unit.INSTANCE;
        });
    }

    private void saveScrollPosition() {
        BreadCrumbLayout.Crumb crumb = getActiveCrumb();
        if (crumb != null) {
            crumb.setScrollPosition(((LinearLayoutManager) viewBinding.recyclerView.getLayoutManager()).findFirstVisibleItemPosition());
        }
    }

    @Nullable
    private BreadCrumbLayout.Crumb getActiveCrumb() {
        if ((viewBinding != null) && (viewBinding.breadCrumbs.size() > 0))
            return viewBinding.breadCrumbs.getCrumb(viewBinding.breadCrumbs.getActiveIndex());
        else
            return null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(CRUMBS, viewBinding.breadCrumbs.getStateWrapper());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = new ViewModelProvider(this).get(FolderFragmentViewModel.class);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            setCrumb(new BreadCrumbLayout.Crumb(FileUtil.safeGetCanonicalFile((File) getArguments().getSerializable(PATH))), true);
        } else {
            viewBinding.breadCrumbs.restoreFromStateWrapper(savedInstanceState.getParcelable(CRUMBS));
            model.loadFiles(getActiveCrumb(),files -> {
                updateAdapter((List<File>) files);
                return Unit.INSTANCE;
            });
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentFolderBinding.inflate(inflater);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        getMainActivity().setStatusbarColorAuto();
        getMainActivity().setNavigationbarColorAuto();
        getMainActivity().setTaskDescriptionColorAuto();

        setUpAppbarColor();
        setUpToolbar();
        setUpBreadCrumbs();
        setUpRecyclerView();
        setUpAdapter();

        model.setRecyclerViewPrepared(true);
    }

    private void setUpAppbarColor() {
        int primaryColor = ThemeColor.primaryColor(getMainActivity());
        viewBinding.appbar.setBackgroundColor(primaryColor);
        viewBinding.toolbar.setBackgroundColor(primaryColor);
        viewBinding.toolbar.setTitleTextColor(ToolbarColorUtil.toolbarTitleColor(requireActivity(), primaryColor));
        viewBinding.breadCrumbs.setBackgroundColor(primaryColor);
        viewBinding.breadCrumbs.setActivatedContentColor(ToolbarColorUtil.toolbarTitleColor(getMainActivity(), primaryColor));
        viewBinding.breadCrumbs.setDeactivatedContentColor(ToolbarColorUtil.toolbarSubtitleColor(getMainActivity(), primaryColor));
    }

    private void setUpToolbar() {
        viewBinding.toolbar.setNavigationIcon(TintHelper.createTintedDrawable(
                AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_menu_white_24dp),
                MaterialColorHelper.getPrimaryTextColor(requireActivity(), ColorUtil.isColorLight(ThemeColor.primaryColor(getMainActivity())))));
        getMainActivity().setTitle(R.string.app_name);
        getMainActivity().setSupportActionBar(viewBinding.toolbar);
    }

    private void setUpBreadCrumbs() {
        viewBinding.breadCrumbs.setCallback(this);
    }

    private void setUpRecyclerView() {
        ViewUtil.setUpFastScrollRecyclerViewColor(getMainActivity(), viewBinding.recyclerView, ThemeColor.accentColor(getMainActivity()));

        viewBinding.recyclerView.setLayoutManager(new LinearLayoutManager(getMainActivity()));

        viewBinding.appbar.addOnOffsetChangedListener(this);
    }

    private void setUpAdapter() {
        adapter = new SongFileAdapter(getMainActivity(), new LinkedList<>(), R.layout.item_list, this, this);

        dataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkIsEmpty();
            }
        };
        // keep observer
        adapter.registerAdapterDataObserver(dataObserver);
        viewBinding.recyclerView.setAdapter(adapter);
        checkIsEmpty();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveScrollPosition();
    }

    @Override
    public void onDestroyView() {
        adapter.unregisterAdapterDataObserver(dataObserver);
        viewBinding.appbar.removeOnOffsetChangedListener(this);
        super.onDestroyView();
        viewBinding = null;
    }

    @Override
    public boolean handleBackPress() {
        if (cab != null && AttachedCabKt.isActive(cab)) {
            AttachedCabKt.destroy(cab);
            return true;
        }
        if (viewBinding.breadCrumbs.popHistory()) {
            setCrumb(viewBinding.breadCrumbs.lastHistory(), false);
            return true;
        }
        return false;
    }


    public AttachedCab showCab(int menuRes,
                               @NonNull Function2<? super AttachedCab, ? super Menu, Unit> createCallback,
                               @NonNull Function1<? super MenuItem, Boolean> selectCallback,
                               @NonNull Function1<? super AttachedCab, Boolean> destroyCallback) {

        if (cab != null && AttachedCabKt.isActive(cab)) AttachedCabKt.destroy(cab);

        cab = MaterialCabKt.createCab(this, R.id.cab_stub, attachedCab -> {
            attachedCab.popupTheme(Setting.instance().getGeneralTheme());
            attachedCab.menu(menuRes);
            attachedCab.closeDrawable(R.drawable.ic_close_white_24dp);
            attachedCab.backgroundColor(null, PhonographColorUtil.shiftBackgroundColorForLightText(ThemeColor.primaryColor(getMainActivity())));
            attachedCab.onCreate(createCallback);
            attachedCab.onSelection(selectCallback);
            attachedCab.onDestroy(destroyCallback);
            return null;
        });

        return cab;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        int primaryColor = ThemeColor.primaryColor(getMainActivity());

        MenuItem scan = menu.add(0, R.id.action_scan, 0, R.string.action_scan_directory);
        scan.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        scan.setIcon(TintHelper.createTintedDrawable(
                AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_scanner_white_24dp),
                MaterialColorHelper.getPrimaryTextColor(requireActivity(), ColorUtil.isColorLight(primaryColor)))
        );

        MenuItem home = menu.add(0, R.id.action_go_to_start_directory, 1, R.string.action_go_to_start_directory);
        home.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        home.setIcon(TintHelper.createTintedDrawable(
                AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_bookmark_music_white_24dp),
                MaterialColorHelper.getPrimaryTextColor(requireActivity(), ColorUtil.isColorLight(primaryColor))
        ));


    }

    public static final FileFilter AUDIO_FILE_FILTER = file -> !file.isHidden() && (file.isDirectory() ||
            FileUtil.fileIsMimeType(file, "audio/*", MimeTypeMap.getSingleton()) ||
            FileUtil.fileIsMimeType(file, "application/ogg", MimeTypeMap.getSingleton()));

    @Override
    public void onCrumbSelection(BreadCrumbLayout.Crumb crumb, int index) {
        setCrumb(crumb, true);
    }

    public static File getDefaultStartDirectory() {
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File startFolder;
        if (musicDir.exists() && musicDir.isDirectory()) {
            startFolder = musicDir;
        } else {
            File externalStorage = Environment.getExternalStorageDirectory();
            if (externalStorage.exists() && externalStorage.isDirectory()) {
                startFolder = externalStorage;
            } else {
                startFolder = new File("/"); // root
            }
        }
        return startFolder;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_go_to_start_directory:
                setCrumb(new BreadCrumbLayout.Crumb(FileUtil.safeGetCanonicalFile(Setting.instance().getStartDirectory())), true);
                return true;
            case R.id.action_scan:
                BreadCrumbLayout.Crumb crumb = getActiveCrumb();
                if (crumb != null) {
                    model.listPaths(
                            new DirectoryInfo(crumb.getFile(), FileScanner.audioFileFilter),
                            paths -> {
                                scanPaths(paths);
                                return Unit.INSTANCE;
                            });
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFileSelected(File file) {
        final File canonicalFile = FileUtil.safeGetCanonicalFile(file); // important as we compare the path value later
        if (canonicalFile.isDirectory()) {
            setCrumb(new BreadCrumbLayout.Crumb(canonicalFile), true);
        } else {
            FileFilter fileFilter = pathname -> !pathname.isDirectory() && AUDIO_FILE_FILTER.accept(pathname);
            new ListSongsAsyncTask(getMainActivity(), null, (songs, extra) -> {
                int startIndex = -1;
                for (int i = 0; i < songs.size(); i++) {
                    if (canonicalFile.getPath().equals(songs.get(i).data)) {
                        startIndex = i;
                        break;
                    }
                }
                if (startIndex > -1) {
                    MusicPlayerRemote.openQueue(songs, startIndex, true);
                } else {
                    Snackbar.make(viewBinding.coordinatorLayout, Html.fromHtml(String.format(getString(R.string.not_listed_in_media_store), canonicalFile.getName()), Html.FROM_HTML_MODE_LEGACY), Snackbar.LENGTH_LONG)
                            .setAction(R.string.action_scan, v -> scanPaths(new String[]{canonicalFile.getPath()}))
                            .setActionTextColor(ThemeColor.accentColor(getMainActivity()))
                            .show();
                }
            }).execute(new ListSongsAsyncTask.LoadingInfo(toList(canonicalFile.getParentFile()), fileFilter, model.getFileComparator()));
        }
    }

    @Override
    public void onMultipleItemAction(MenuItem item, List<? extends File> files) {
        final int itemId = item.getItemId();
        new ListSongsAsyncTask(getMainActivity(), null, (songs, extra) -> {
            if (!songs.isEmpty()) {
                SongsMenuHelper.handleMenuClick(getMainActivity(), songs, itemId);
            }
            if (songs.size() != files.size()) {
                Snackbar.make(viewBinding.coordinatorLayout, R.string.some_files_are_not_listed_in_the_media_store, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_scan, v -> {
                            String[] paths = new String[files.size()];
                            for (int i = 0; i < files.size(); i++) {
                                paths[i] = FileUtil.safeGetCanonicalPath(files.get(i));
                            }
                            scanPaths(paths);
                        })
                        .setActionTextColor(ThemeColor.accentColor(getMainActivity()))
                        .show();
            }
        }).execute(new ListSongsAsyncTask.LoadingInfo((List<File>) files, AUDIO_FILE_FILTER, model.getFileComparator()));
    }

    private List<File> toList(File file) {
        List<File> files = new ArrayList<>(1);
        files.add(file);
        return files;
    }

//    Comparator<File> fileComparator = (lhs, rhs) -> {
//        if (lhs.isDirectory() && !rhs.isDirectory()) {
//            return -1;
//        } else if (!lhs.isDirectory() && rhs.isDirectory()) {
//            return 1;
//        } else {
//            return lhs.getName().compareToIgnoreCase
//                    (rhs.getName());
//        }
//    };
//
//    private Comparator<File> getFileComparator() {
//        return fileComparator;
//    }

    @Override
    public void onFileMenuClicked(final File file, View view) {
        PopupMenu popupMenu = new PopupMenu(getMainActivity(), view);
        if (file.isDirectory()) {
            popupMenu.inflate(R.menu.menu_item_directory);
            popupMenu.setOnMenuItemClickListener(item -> {
                final int itemId = item.getItemId();
                switch (itemId) {
                    case R.id.action_play_next:
                    case R.id.action_add_to_current_playing:
                    case R.id.action_add_to_playlist:
                    case R.id.action_delete_from_device:
                        new ListSongsAsyncTask(getMainActivity(), null, (songs, extra) -> {
                            if (!songs.isEmpty()) {
                                SongsMenuHelper.handleMenuClick(getMainActivity(), songs, itemId);
                            }
                        }).execute(new ListSongsAsyncTask.LoadingInfo(toList(file), AUDIO_FILE_FILTER, model.getFileComparator()));
                        return true;
                    case R.id.action_set_as_start_directory:
                        Setting.instance().setStartDirectory(file);
                        Toast.makeText(getMainActivity(), String.format(getString(R.string.new_start_directory), file.getPath()), Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.action_scan:
                        model.listPaths(
                                new DirectoryInfo(file, FileScanner.audioFileFilter),
                                paths -> {
                                    scanPaths(paths);
                                    return Unit.INSTANCE;
                                });
                        return true;
                    case R.id.action_add_to_black_list:
                        BlacklistUtil.INSTANCE.addToBlacklist(requireActivity(), file);
                        return true;
                }
                return false;
            });
        } else {
            popupMenu.inflate(R.menu.menu_item_file);
            popupMenu.setOnMenuItemClickListener(item -> {
                final int itemId = item.getItemId();
                switch (itemId) {
                    case R.id.action_play_next:
                    case R.id.action_add_to_current_playing:
                    case R.id.action_add_to_playlist:
                    case R.id.action_go_to_album:
                    case R.id.action_go_to_artist:
                    case R.id.action_share:
                    case R.id.action_tag_editor:
                    case R.id.action_details:
                    case R.id.action_set_as_ringtone:
                    case R.id.action_add_to_black_list:
                    case R.id.action_delete_from_device:
                        new ListSongsAsyncTask(getMainActivity(), null, (songs, extra) -> {
                            if (!songs.isEmpty()) {
                                SongMenuHelper.handleMenuClick(getMainActivity(), songs.get(0), itemId);
                            } else {
                                Snackbar.make(viewBinding.coordinatorLayout, Html.fromHtml(String.format(getString(R.string.not_listed_in_media_store), file.getName()), Html.FROM_HTML_MODE_LEGACY), Snackbar.LENGTH_LONG)
                                        .setAction(R.string.action_scan, v -> scanPaths(new String[]{FileUtil.safeGetCanonicalPath(file)}))
                                        .setActionTextColor(ThemeColor.accentColor(getMainActivity()))
                                        .show();
                            }
                        }).execute(new ListSongsAsyncTask.LoadingInfo(toList(file), AUDIO_FILE_FILTER, model.getFileComparator()));
                        return true;
                    case R.id.action_scan:
                        scanPaths(new String[]{FileUtil.safeGetCanonicalPath(file)});
                        return true;
                }
                return false;
            });
        }
        popupMenu.show();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        viewBinding.container.setPadding(viewBinding.container.getPaddingLeft(), viewBinding.container.getPaddingTop(), viewBinding.container.getPaddingRight(), viewBinding.appbar.getTotalScrollRange() + verticalOffset);
    }

    private void checkIsEmpty() {
        viewBinding.empty.setVisibility(adapter == null || adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void scanPaths(@Nullable String[] toBeScanned) {
        if (toBeScanned == null || toBeScanned.length < 1) {
            Toast.makeText(requireActivity(), R.string.nothing_to_scan, Toast.LENGTH_SHORT).show();
        } else {
            MediaScannerConnection.scanFile(App.getInstance(), toBeScanned, null, new UpdateToastMediaScannerCompletionListener(requireActivity(), toBeScanned));
        }
    }

    private void updateAdapter(@NonNull List<File> files) {
        adapter.setDataSet(files);
        BreadCrumbLayout.Crumb crumb = getActiveCrumb();
        if (crumb != null) {
            ((LinearLayoutManager) viewBinding.recyclerView.getLayoutManager()).scrollToPositionWithOffset(crumb.getScrollPosition(), 0);
        }
    }

    private static class ListSongsAsyncTask extends ListingFilesDialogAsyncTask<ListSongsAsyncTask.LoadingInfo, Void, List<Song>> {
        private WeakReference<Context> contextWeakReference;
        private WeakReference<OnSongsListedCallback> callbackWeakReference;
        private final Object extra;

        public ListSongsAsyncTask(Context context, Object extra, OnSongsListedCallback callback) {
            super(context, 500);
            this.extra = extra;
            contextWeakReference = new WeakReference<>(context);
            callbackWeakReference = new WeakReference<>(callback);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            checkCallbackReference();
            checkContextReference();
        }

        @Override
        protected List<Song> doInBackground(LoadingInfo... params) {
            try {
                LoadingInfo info = params[0];
                List<File> files = FileUtil.listFilesDeep(info.files, info.fileFilter);

                if (isCancelled() || checkContextReference() == null || checkCallbackReference() == null)
                    return null;

                Collections.sort(files, info.fileComparator);

                Context context = checkContextReference();
                if (isCancelled() || context == null || checkCallbackReference() == null)
                    return null;

                return FileUtil.matchFilesWithMediaStore(context, files);
            } catch (Exception e) {
                e.printStackTrace();
                cancel(false);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Song> songs) {
            super.onPostExecute(songs);
            OnSongsListedCallback callback = checkCallbackReference();
            if (songs != null && callback != null)
                callback.onSongsListed(songs, extra);
        }

        private Context checkContextReference() {
            Context context = contextWeakReference.get();
            if (context == null) {
                cancel(false);
            }
            return context;
        }

        private OnSongsListedCallback checkCallbackReference() {
            OnSongsListedCallback callback = callbackWeakReference.get();
            if (callback == null) {
                cancel(false);
            }
            return callback;
        }

        public static class LoadingInfo {
            public final Comparator<File> fileComparator;
            public final FileFilter fileFilter;
            public final List<File> files;

            public LoadingInfo(@NonNull List<File> files, @NonNull FileFilter fileFilter, @NonNull Comparator<File> fileComparator) {
                this.fileComparator = fileComparator;
                this.fileFilter = fileFilter;
                this.files = files;
            }
        }

        public interface OnSongsListedCallback {
            void onSongsListed(@NonNull List<Song> songs, Object extra);
        }
    }

    private static abstract class ListingFilesDialogAsyncTask<Params, Progress, Result> extends DialogAsyncTask<Params, Progress, Result> {
        public ListingFilesDialogAsyncTask(Context context) {
            super(context);
        }

        public ListingFilesDialogAsyncTask(Context context, int showDelay) {
            super(context, showDelay);
        }

        @Override
        protected Dialog createDialog(@NonNull Context context) {
            MaterialDialog dialog = new MaterialDialog(context, MaterialDialog.getDEFAULT_BEHAVIOR());
            dialog.title(R.string.listing_files, null);
            dialog.cancelable(true);
            dialog.negativeButton(android.R.string.cancel, null, dialog1 -> {
                cancel(false);
                return null;
            });
            //set button color
            DialogActionExtKt.getActionButton(dialog, WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(context));
            return dialog;
        }
    }
}
