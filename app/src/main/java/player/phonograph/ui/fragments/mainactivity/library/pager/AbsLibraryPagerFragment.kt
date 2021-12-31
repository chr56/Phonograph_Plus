package player.phonograph.ui.fragments.mainactivity.library.pager

import android.os.Bundle
import androidx.loader.app.LoaderManager
import player.phonograph.ui.fragments.AbsMusicServiceFragment
import player.phonograph.ui.fragments.mainactivity.library.LibraryFragment

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
open class AbsLibraryPagerFragment : AbsMusicServiceFragment() {
    override fun getLoaderManager(): LoaderManager = parentFragment!!.loaderManager

    val libraryFragment: LibraryFragment? get() = parentFragment as LibraryFragment?

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }
}
