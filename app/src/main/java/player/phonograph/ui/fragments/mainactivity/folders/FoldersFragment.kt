package player.phonograph.ui.fragments.mainactivity.folders

import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileFilter
import java.util.*
import player.phonograph.App
import player.phonograph.R
import player.phonograph.adapter.SongFileAdapter
import player.phonograph.databinding.FragmentFolderBinding
import player.phonograph.helper.menu.SongsMenuHelper
import player.phonograph.interfaces.CabHolder
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote.openQueue
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.MainActivity.MainActivityFragmentCallbacks
import player.phonograph.ui.fragments.mainactivity.AbsMainActivityFragment
import player.phonograph.util.BlacklistUtil
import player.phonograph.util.FileUtil.safeGetCanonicalFile
import player.phonograph.util.FileUtil.safeGetCanonicalPath
import player.phonograph.util.PhonographColorUtil.shiftBackgroundColorForLightText
import player.phonograph.util.ViewUtil.setUpFastScrollRecyclerViewColor
import player.phonograph.views.BreadCrumbLayout
import player.phonograph.views.BreadCrumbLayout.Crumb
import util.mdcolor.ColorUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.MaterialColorHelper
import util.mddesign.util.TintHelper
import util.mddesign.util.ToolbarColorUtil

class FoldersFragment :
    AbsMainActivityFragment(),
    MainActivityFragmentCallbacks,
    CabHolder,
    BreadCrumbLayout.SelectionCallback,
    SongFileAdapter.Callbacks,
    AppBarLayout.OnOffsetChangedListener {

    private var _viewBinding: FragmentFolderBinding? = null
    private val viewBinding: FragmentFolderBinding get() = _viewBinding!!

    private val model: FoldersFragmentViewModel by viewModels()

    private lateinit var adapter: SongFileAdapter
    private lateinit var dataObserver: AdapterDataObserver

    private var cab: AttachedCab? = null

    private fun setCrumb(crumb: Crumb, addToHistory: Boolean) {

        saveScrollPosition()

        viewBinding.breadCrumbs.setActiveOrAdd(crumb, false)
        if (addToHistory) viewBinding.breadCrumbs.addHistory(crumb)
        model.listDirectoriesAndFiles(crumb) { files: Array<File>? ->
            updateAdapter(files)
        }
    }

    private fun saveScrollPosition() {
        val crumb = activeCrumb
        if (crumb != null) {
            crumb.scrollPosition =
                (viewBinding.recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        }
    }

    private val activeCrumb: Crumb?
        get() =
            if (_viewBinding != null && viewBinding.breadCrumbs.size() > 0)
                viewBinding.breadCrumbs.getCrumb(viewBinding.breadCrumbs.activeIndex)
            else null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(CRUMBS, viewBinding.breadCrumbs.stateWrapper)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            setCrumb(Crumb(safeGetCanonicalFile((requireArguments().getSerializable(PATH) as File))), true)
        } else {
            viewBinding.breadCrumbs.restoreFromStateWrapper(savedInstanceState.getParcelable(CRUMBS))
            model.listDirectoriesAndFiles(activeCrumb) { files: Array<File>? ->
                updateAdapter(files)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _viewBinding = FragmentFolderBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        mainActivity.setStatusbarColorAuto()
        mainActivity.setNavigationbarColorAuto()
        mainActivity.setTaskDescriptionColorAuto()

        setUpAppbarColor()
        setUpToolbar()
        setUpBreadCrumbs()
        setUpRecyclerView()

        model.isRecyclerViewPrepared = true
    }

    private fun setUpAppbarColor() {
        val primaryColor = ThemeColor.primaryColor(mainActivity)
        viewBinding.appbar.setBackgroundColor(primaryColor)
        viewBinding.toolbar.setBackgroundColor(primaryColor)
        viewBinding.toolbar.setTitleTextColor(ToolbarColorUtil.toolbarTitleColor(requireActivity(), primaryColor))
        viewBinding.breadCrumbs.setBackgroundColor(primaryColor)
        viewBinding.breadCrumbs.setActivatedContentColor(ToolbarColorUtil.toolbarTitleColor(mainActivity, primaryColor))
        viewBinding.breadCrumbs.setDeactivatedContentColor(ToolbarColorUtil.toolbarSubtitleColor(mainActivity, primaryColor))
    }

    private fun setUpToolbar() {
        viewBinding.toolbar.navigationIcon = TintHelper.createTintedDrawable(
            AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_menu_white_24dp),
            MaterialColorHelper.getPrimaryTextColor(requireActivity(), ColorUtil.isColorLight(ThemeColor.primaryColor(mainActivity)))
        )
        mainActivity.setTitle(R.string.app_name)
        mainActivity.setSupportActionBar(viewBinding.toolbar)
    }

    private fun setUpBreadCrumbs() {
        viewBinding.breadCrumbs.setCallback(this)
    }

    private fun setUpRecyclerView() {
        setUpFastScrollRecyclerViewColor(mainActivity, viewBinding.recyclerView, ThemeColor.accentColor(mainActivity))

        viewBinding.recyclerView.layoutManager = LinearLayoutManager(mainActivity)
        viewBinding.appbar.addOnOffsetChangedListener(this)

        adapter = SongFileAdapter(mainActivity, LinkedList(), R.layout.item_list, this, this)
        dataObserver = object : AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        }
        // keep observer
        adapter.registerAdapterDataObserver(dataObserver)
        viewBinding.recyclerView.adapter = adapter
        checkIsEmpty()
    }

    override fun onPause() {
        super.onPause()
        saveScrollPosition()
    }

    override fun onDestroyView() {
        adapter.unregisterAdapterDataObserver(dataObserver)
        viewBinding.appbar.removeOnOffsetChangedListener(this)
        super.onDestroyView()
        _viewBinding = null
    }

    override fun handleBackPress(): Boolean {
        if (cab != null && cab.isActive()) {
            cab.destroy()
            return true
        }
        if (viewBinding.breadCrumbs.popHistory()) {
            setCrumb(viewBinding.breadCrumbs.lastHistory(), false)
            return true
        }
        return false
    }

    override fun showCab(
        menuRes: Int,
        createCallback: Function2<AttachedCab, Menu, Unit>,
        selectCallback: Function1<MenuItem, Boolean>,
        destroyCallback: Function1<AttachedCab, Boolean>,
    ): AttachedCab {
        if (cab != null && cab.isActive()) cab.destroy()

        cab = this.createCab(R.id.cab_stub) {
            popupTheme(Setting.instance().generalTheme)
            menu(menuRes)
            closeDrawable(R.drawable.ic_close_white_24dp)
            backgroundColor(null, shiftBackgroundColorForLightText(ThemeColor.primaryColor(mainActivity)))
            onCreate(createCallback)
            onSelection(selectCallback)
            onDestroy(destroyCallback)
        }

        return cab!!
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val primaryColor = ThemeColor.primaryColor(mainActivity)

        menu.add(0, R.id.action_scan, 0, R.string.action_scan_directory).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            icon = TintHelper.createTintedDrawable(
                AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_scanner_white_24dp),
                MaterialColorHelper.getPrimaryTextColor(requireActivity(), ColorUtil.isColorLight(primaryColor))
            )
        }

        menu.add(0, R.id.action_go_to_start_directory, 1, R.string.action_go_to_start_directory).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            icon = TintHelper.createTintedDrawable(
                AppCompatResources.getDrawable(requireActivity(), R.drawable.ic_bookmark_music_white_24dp),
                MaterialColorHelper.getPrimaryTextColor(requireActivity(), ColorUtil.isColorLight(primaryColor))
            )
        }
    }

    override fun onCrumbSelection(crumb: Crumb, index: Int) = setCrumb(crumb, true)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_go_to_start_directory -> {
                setCrumb(Crumb(safeGetCanonicalFile(Setting.instance().startDirectory)), true)
                return true
            }
            R.id.action_scan -> {
                activeCrumb?.let {
                    model.scanPaths(DirectoryInfo(it.file, FileScanner.audioFileFilter)) { paths: Array<String>? ->
                        model.scanSongFiles(requireActivity(), paths)
                    }
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onFileSelected(file: File) {
        val canonicalFile = safeGetCanonicalFile(file) // important as we compare the path value later
        if (canonicalFile.isDirectory) {
            setCrumb(Crumb(canonicalFile), true)
        } else {
            val fileFilter = FileFilter { pathname: File -> !pathname.isDirectory && FileScanner.audioFileFilter.accept(pathname) }
            canonicalFile.parentFile?.let {
                model.searchSongs(
                    FileInfo(listOf(it), fileFilter), { songs: List<Song>?, _: Any? ->
                    val makeSnackBar: () -> Unit = {
                        Snackbar.make(
                            viewBinding.coordinatorLayout,
                            Html.fromHtml(
                                String.format(getString(R.string.not_listed_in_media_store), canonicalFile.name),
                                Html.FROM_HTML_MODE_LEGACY
                            ),
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(R.string.action_scan) { model.scanSongFiles(requireActivity(), arrayOf(canonicalFile.path)) }
                            .setActionTextColor(ThemeColor.accentColor(mainActivity))
                            .show()
                    }

                    if (songs == null) {
                        makeSnackBar()
                        return@searchSongs
                    }
                    var startIndex = -1
                    for (index in songs.indices) {
                        if (canonicalFile.path == songs[index].data) {
                            startIndex = index
                            break
                        }
                    }
                    if (startIndex > -1) {
                        openQueue(songs, startIndex, true)
                    } else {
                        makeSnackBar()
                    }
                }
                )
            }
        }
    }

    override fun onMultipleItemAction(item: MenuItem, files: List<File>) {
        val itemId = item.itemId
        model.searchSongs(
            FileInfo(files, FileScanner.audioFileFilter), { songs: List<Song>?, _: Any? ->
            if (!songs.isNullOrEmpty()) {
                SongsMenuHelper.handleMenuClick(mainActivity, songs, itemId)
            }
            if (songs?.size ?: 0 != files.size) {
                Snackbar.make(
                    viewBinding.coordinatorLayout,
                    R.string.some_files_are_not_listed_in_the_media_store,
                    Snackbar.LENGTH_LONG
                )
                    .setAction(R.string.action_scan) {
                        val paths = files.map { safeGetCanonicalPath(it) }.toTypedArray()
                        model.scanSongFiles(requireActivity(), paths)
                    }
                    .setActionTextColor(ThemeColor.accentColor(mainActivity))
                    .show()
            }
            Unit
        }
        )
    }

    override fun onFileMenuClicked(file: File, view: View?) {
        val popupMenu = PopupMenu(mainActivity, view)
        if (file.isDirectory) {
            popupMenu.inflate(R.menu.menu_item_directory)
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (val itemId = item.itemId) {
                    R.id.action_play_next, R.id.action_add_to_current_playing, R.id.action_add_to_playlist, R.id.action_delete_from_device -> {
                        model.searchSongs(
                            FileInfo(listOf(file), FileScanner.audioFileFilter),
                            { songs: List<Song>?, _: Any? ->
                                if (!songs.isNullOrEmpty()) {
                                    SongsMenuHelper.handleMenuClick(mainActivity, songs, itemId)
                                }
                                Unit
                            }
                        )
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_set_as_start_directory -> {
                        Setting.instance().startDirectory = file
                        Toast.makeText(mainActivity, String.format(getString(R.string.new_start_directory), file.path), Toast.LENGTH_SHORT)
                            .show()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_scan -> {
                        model.scanPaths(DirectoryInfo(file, FileScanner.audioFileFilter)) { paths: Array<String>? ->
                            if (!paths.isNullOrEmpty()) model.scanSongFiles(requireActivity(), paths)
                        }
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_add_to_black_list -> {
                        BlacklistUtil.addToBlacklist(requireActivity(), file)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
        } else {
            popupMenu.inflate(R.menu.menu_item_file)
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (val itemId = item.itemId) {
                    R.id.action_play_next, R.id.action_add_to_current_playing, R.id.action_add_to_playlist, R.id.action_go_to_album, R.id.action_go_to_artist, R.id.action_share, R.id.action_tag_editor, R.id.action_details, R.id.action_set_as_ringtone, R.id.action_add_to_black_list, R.id.action_delete_from_device -> {
                        model.searchSongs(
                            FileInfo(listOf(file), FileScanner.audioFileFilter),
                            { songs: List<Song>?, _: Any? ->
                                if (!songs.isNullOrEmpty()) {
                                    SongsMenuHelper.handleMenuClick(mainActivity, songs, itemId)
                                } else {
                                    Snackbar.make(
                                        viewBinding.coordinatorLayout,
                                        Html.fromHtml(
                                            String.format(getString(R.string.not_listed_in_media_store), file.name),
                                            Html.FROM_HTML_MODE_LEGACY
                                        ),
                                        Snackbar.LENGTH_LONG
                                    )
                                        .setAction(R.string.action_scan) {
                                            model.scanSongFiles(
                                                requireActivity(),
                                                arrayOf(safeGetCanonicalPath(file))
                                            )
                                        }
                                        .setActionTextColor(ThemeColor.accentColor(mainActivity))
                                        .show()
                                }
                                Unit
                            }
                        )
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_scan -> {
                        model.scanSongFiles(requireActivity(), arrayOf(safeGetCanonicalPath(file)))
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }
        }
        popupMenu.show()
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        viewBinding.container.setPadding(
            viewBinding.container.paddingLeft,
            viewBinding.container.paddingTop,
            viewBinding.container.paddingRight,
            viewBinding.appbar.totalScrollRange + verticalOffset
        )
    }

    private fun checkIsEmpty() {
        viewBinding.empty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun updateAdapter(files: Array<File>?) {
        adapter.dataSet = files?.toList() ?: emptyList()
        activeCrumb?.let {
            (viewBinding.recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(it.scrollPosition, 0)
        }
    }

    companion object {

        private const val PATH = "path"
        private const val CRUMBS = "crumbs"

        fun newInstance(): FoldersFragment {
            return newInstance(Setting.instance().startDirectory)
        }

        fun newInstance(directory: File): FoldersFragment {
            return FoldersFragment().apply { arguments = Bundle().apply { putSerializable(PATH, directory) } }
        }

        // root
        val defaultStartDirectory: File
            get() {
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)

                return if (musicDir != null && musicDir.exists() && musicDir.isDirectory) {
                    musicDir
                } else {
                    val externalStorage = Environment.getExternalStorageDirectory()
                    if (externalStorage.exists() && externalStorage.isDirectory) {
                        externalStorage
                    } else {
                        App.instance.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File("/") // root
                    }
                }
            }
    }
}
