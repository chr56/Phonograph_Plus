package com.kabouzeid.gramophone.ui.fragments.mainactivity.library.pager;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;

import com.kabouzeid.gramophone.ui.fragments.AbsMusicServiceFragment;
import com.kabouzeid.gramophone.ui.fragments.mainactivity.library.LibraryFragment;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class AbsLibraryPagerFragment extends AbsMusicServiceFragment {

    /* http://stackoverflow.com/a/2888433 */
    @NonNull
    @Override
    public LoaderManager getLoaderManager() {
        return getParentFragment().getLoaderManager();
    }

    public LibraryFragment getLibraryFragment() {
        return (LibraryFragment) getParentFragment();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }
}
