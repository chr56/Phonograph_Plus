package player.phonograph.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import chr_56.MDthemer.core.ThemeColor
import chr_56.MDthemer.util.ColorUtil
import chr_56.MDthemer.util.NavigationViewUtil
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.coroutines.*
import player.phonograph.*
import player.phonograph.Updater.checkUpdate
import player.phonograph.dialogs.ChangelogDialog.Companion.create
import player.phonograph.dialogs.ChangelogDialog.Companion.setChangelogRead
import player.phonograph.dialogs.ScanMediaFolderDialog
import player.phonograph.dialogs.UpgradeDialog
import player.phonograph.glide.SongGlideRequest
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.helper.SearchQueryHelper
import player.phonograph.loader.AlbumLoader
import player.phonograph.loader.ArtistLoader
import player.phonograph.loader.PlaylistSongLoader
import player.phonograph.loader.SongLoader
import player.phonograph.model.Song
import player.phonograph.model.smartplaylist.HistoryPlaylist
import player.phonograph.model.smartplaylist.LastAddedPlaylist
import player.phonograph.model.smartplaylist.MyTopTracksPlaylist
import player.phonograph.notification.UpgradeNotification
import player.phonograph.service.MusicService
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.activities.intro.AppIntroActivity
import player.phonograph.ui.fragments.mainactivity.AbsMainActivityFragment
import player.phonograph.ui.fragments.mainactivity.folders.FoldersFragment
import player.phonograph.ui.fragments.mainactivity.library.LibraryFragment
import player.phonograph.ui.fragments.mainactivity.library.new_ui.HomeFragment
import player.phonograph.util.FileSaver
import player.phonograph.util.MusicUtil
import player.phonograph.util.PreferenceUtil
import chr_56.MDthemer.util.Util as MDthemerUtil

class MainActivity : AbsSlidingMusicPanelActivity() {
    // init : onCreate()
    private lateinit var navigationView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var floatingActionButton: FloatingActionButton

    private lateinit var currentFragment: MainActivityFragmentCallbacks
    private var navigationDrawerHeader: View? = null
    private var blockRequestPermissions = false

    private var savedMessageBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setDrawUnderStatusbar()

        // todo: viewBinding
        navigationView = findViewById(R.id.navigation_view)
        drawerLayout = findViewById(R.id.drawer_layout)
        floatingActionButton = findViewById(R.id.add_new_item)

        setUpDrawer()

        if (savedInstanceState == null) {
            setMusicChooser(PreferenceUtil.getInstance(this).lastMusicChooser)
        } else {
            currentFragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container) as MainActivityFragmentCallbacks
        }

        Log.d("MainActivity", "StartIntent:$intent: UPGRADABLE-${intent.getBooleanExtra(UPGRADABLE, false)}")
        if (intent.getBooleanExtra(UPGRADABLE, false)) {
            Log.d("Updater", "receive upgradable notification intent!")
            showUpgradeDialog(intent.getBundleExtra(VERSION_INFO)!!)
        }

        setupHandler()

        showIntro()
        checkUpdate()
        showChangelog()

        setUpFloatingActionButton()
    }

    override fun createContentView(): View {
        @SuppressLint("InflateParams")
        val contentView =
            layoutInflater.inflate(R.layout.activity_main_drawer_layout, null)
        val drawerContent = contentView.findViewById<ViewGroup>(R.id.drawer_content_container)
        drawerContent.addView(wrapSlidingMusicPanel(R.layout.activity_main_content))
        return contentView
    }

    private fun setupHandler() {
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    REQUEST_CODE_SAVE_PLAYLIST -> {
                        savedMessageBundle = msg.data
                        // just save message bundle, then wait for uri
                    }
                }
            }
        }
    }

    private fun setMusicChooser(key: Int) {
        PreferenceUtil.getInstance(this).lastMusicChooser = key
        when (key) {
            LIBRARY -> {
                navigationView.setCheckedItem(R.id.nav_library)
                setCurrentFragment(LibraryFragment.newInstance())
            }
            FOLDERS -> {
                navigationView.setCheckedItem(R.id.nav_folders)
                setCurrentFragment(FoldersFragment.newInstance(this))
            }
            HOME -> {
                navigationView.setCheckedItem(R.id.nav_home)
                setCurrentFragment(HomeFragment.newInstance())
            }
        }
    }

    private fun setCurrentFragment(fragment: AbsMainActivityFragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, null)
            .commit()
        currentFragment = fragment as MainActivityFragmentCallbacks
    }

    private val backgroundCoroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_INTRO_REQUEST) {
            blockRequestPermissions = false
            if (!hasPermissions()) {
                requestPermissions()
            }
            create().show(supportFragmentManager, "CHANGE_LOG_DIALOG")
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SAVE_PLAYLIST) {
            data?.let { intent ->
                val uri = intent.data!!

                val bundle = savedMessageBundle ?: throw Exception("No Playlist to save?")

                backgroundCoroutineScope.launch(Dispatchers.IO) {
                    val result = when (bundle.getString(TYPE)!!) {
                        NormalPlaylist ->
                            bundle.getLong(PLAYLIST_ID).let { FileSaver.savePlaylist(this@MainActivity, uri, it) }
                        MyTopTracksPlaylist ->
                            FileSaver.savePlaylist(this@MainActivity, uri, MyTopTracksPlaylist(this@MainActivity))
                        LastAddedPlaylist ->
                            FileSaver.savePlaylist(this@MainActivity, uri, LastAddedPlaylist(this@MainActivity))
                        HistoryPlaylist ->
                            FileSaver.savePlaylist(this@MainActivity, uri, HistoryPlaylist(this@MainActivity))
                        else -> throw Exception("Unknown Playlist Type: ${bundle.getString(TYPE)}")
                    }

                    // report result
                    val text =
                        if (result.await() == 0) getText(R.string.success) else getText(R.string.failed)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun requestPermissions() {
        if (!blockRequestPermissions) super.requestPermissions()
    }

    private fun setUpDrawer() {
        val accentColor = ThemeColor.accentColor(this)
        NavigationViewUtil.setItemIconColors(
            navigationView, MDthemerUtil.resolveColor(this, R.attr.iconColor, ThemeColor.textColorSecondary(this)), accentColor
        )
        NavigationViewUtil.setItemTextColors(
            navigationView, ThemeColor.textColorPrimary(this), accentColor
        )

        navigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            drawerLayout.closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_library -> Handler().postDelayed({ setMusicChooser(LIBRARY) }, 200)
                R.id.nav_folders -> Handler().postDelayed({ setMusicChooser(FOLDERS) }, 200)
                R.id.nav_home -> Handler().postDelayed({ setMusicChooser(HOME) }, 200)

                R.id.action_shuffle_all -> Handler().postDelayed({
                    MusicPlayerRemote.openAndShuffleQueue(SongLoader.getAllSongs(this), true)
                }, 350)
                R.id.action_scan -> Handler().postDelayed({
                    ScanMediaFolderDialog().show(supportFragmentManager, "SCAN_MEDIA_FOLDER_CHOOSER")
                }, 200)
                R.id.theme_toggle -> Handler().postDelayed({
                    val themeSetting = PreferenceUtil.getInstance(this).generalTheme

                    if (themeSetting == R.style.Theme_Phonograph_Auto) {
                        Toast.makeText(this, R.string.auto_mode_on, Toast.LENGTH_SHORT).show()
                    } else {
                        when (themeSetting) {
                            R.style.Theme_Phonograph_Light ->
                                PreferenceUtil.getInstance(this).setGeneralTheme("dark")
                            R.style.Theme_Phonograph_Dark, R.style.Theme_Phonograph_Black ->
                                PreferenceUtil.getInstance(this).setGeneralTheme("light")
                        }
                        recreate()
                    }
                }, 200)

                R.id.nav_settings -> Handler().postDelayed({
                    startActivity(Intent(this, SettingsActivity::class.java))
                }, 200)
                R.id.nav_about -> Handler().postDelayed({
                    startActivity(Intent(this, AboutActivity::class.java))
                }, 200)
            }
            true
        }
    }

    private fun updateNavigationDrawerHeader() {
        if (MusicPlayerRemote.getPlayingQueue().isNotEmpty()) {
            val song = MusicPlayerRemote.getCurrentSong()

            if (navigationDrawerHeader == null) {
                navigationDrawerHeader =
                    navigationView.inflateHeaderView(R.layout.navigation_drawer_header)
                (navigationDrawerHeader as View).setOnClickListener {
                    drawerLayout.closeDrawers()
                    if (panelState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                        expandPanel()
                    }
                }
            }

            (navigationDrawerHeader!!.findViewById<View>(R.id.title) as TextView).text = song.title
            (navigationDrawerHeader!!.findViewById<View>(R.id.text) as TextView).text =
                MusicUtil.getSongInfoString(song)
            SongGlideRequest.Builder.from(Glide.with(this), song)
                .checkIgnoreMediaStore(this).build()
                .into(navigationDrawerHeader!!.findViewById<View>(R.id.image) as ImageView)
        } else {
            if (navigationDrawerHeader != null) {
                navigationView.removeHeaderView(navigationDrawerHeader!!)
                navigationDrawerHeader = null
            }
        }
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateNavigationDrawerHeader()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateNavigationDrawerHeader()

        intent?.let { handlePlaybackIntent(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(navigationView)) {
                drawerLayout.closeDrawer(navigationView)
            } else {
                drawerLayout.openDrawer(navigationView)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun handleBackPress(): Boolean {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers()
            return true
        }
        return super.handleBackPress() || currentFragment.handleBackPress()
    }

    private fun handlePlaybackIntent(intent: Intent) {
        var handled = false

        intent.action?.let {
            if (it == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
                val songs = SearchQueryHelper.getSongs(this, intent.extras!!)
                if (MusicPlayerRemote.getShuffleMode() == MusicService.SHUFFLE_MODE_SHUFFLE) {
                    MusicPlayerRemote.openAndShuffleQueue(songs, true)
                } else {
                    MusicPlayerRemote.openQueue(songs, 0, true)
                }
                handled = true
            }
        }

        val uri = intent.data
        if (uri != null && uri.toString().isNotEmpty()) {
            MusicPlayerRemote.playFromUri(uri)
            handled = true
        } else {
            when (intent.type) {
                MediaStore.Audio.Playlists.CONTENT_TYPE -> {
                    val id = parseIdFromIntent(intent, "playlistId", "playlist")
                    if (id >= 0) {
                        val position = intent.getIntExtra("position", 0)
                        val songs: List<Song> =
                            ArrayList<Song>(PlaylistSongLoader.getPlaylistSongList(this, id))
                        MusicPlayerRemote.openQueue(songs, position, true)
                        handled = true
                    }
                }
                MediaStore.Audio.Albums.CONTENT_TYPE -> {
                    val id = parseIdFromIntent(intent, "albumId", "album")
                    if (id >= 0) {
                        val position = intent.getIntExtra("position", 0)
                        MusicPlayerRemote.openQueue(AlbumLoader.getAlbum(this, id).songs, position, true)
                        handled = true
                    }
                }
                MediaStore.Audio.Artists.CONTENT_TYPE -> {
                    val id = parseIdFromIntent(intent, "artistId", "artist")
                    if (id >= 0) {
                        val position = intent.getIntExtra("position", 0)
                        MusicPlayerRemote.openQueue(ArtistLoader.getArtist(this, id).songs, position, true)
                        handled = true
                    }
                }
            }
        }

        if (handled) setIntent(Intent())
    }

    private fun parseIdFromIntent(intent: Intent, longKey: String, stringKey: String): Long {
        var id = intent.getLongExtra(longKey, -1)
        if (id < 0) {
            val idString = intent.getStringExtra(stringKey)
            if (idString != null) {
                try {
                    id = idString.toLong()
                } catch (e: NumberFormatException) {
                    e.message?.let { Log.e(TAG, it) }
                }
            }
        }
        return id
    }

    override fun onPanelExpanded(view: View) {
        super.onPanelExpanded(view)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onPanelCollapsed(view: View) {
        super.onPanelCollapsed(view)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private fun showIntro() {
        if (!PreferenceUtil.getInstance(this).introShown()) {
            PreferenceUtil.getInstance(this).setIntroShown()
            setChangelogRead(this)

            blockRequestPermissions = true

            Handler().postDelayed({
                startActivityForResult(Intent(this@MainActivity, AppIntroActivity::class.java), APP_INTRO_REQUEST)
            }, 50)
        }
    }

    private fun checkUpdate() {
        Handler(Looper.getMainLooper()).postDelayed(
            { checkUpdate(callback = { UpgradeNotification.sendUpgradeNotification(it) }, false) }, 3000
        )
    }

    private fun showChangelog() {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersion = pInfo.versionCode
            if (currentVersion != PreferenceUtil.getInstance(this).getLastChangelogVersion()) {
                create().show(supportFragmentManager, "CHANGE_LOG_DIALOG")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun showUpgradeDialog(versionInfo: Bundle) {
        UpgradeDialog.create(versionInfo).show(supportFragmentManager, "UpgradeDialog")
    }

    interface MainActivityFragmentCallbacks {
        fun handleBackPress(): Boolean
        fun handleFloatingActionButtonPress(): Boolean
    }

    private fun setUpFloatingActionButton() {
        val primaryColor = ThemeColor.primaryColor(this)
        val accentColor = ThemeColor.accentColor(this)

        floatingActionButton.backgroundTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(),
            ),
            intArrayOf(
                ColorUtil.lightenColor(primaryColor), accentColor, primaryColor
            )
        )
//        floatingActionButton.rippleColor = accentColor

        floatingActionButton.setOnClickListener { currentFragment.handleFloatingActionButtonPress() }
    }
    fun setFloatingActionButtonVisibility(visibility: Int) {
        floatingActionButton.visibility = visibility
    }

    override fun onDestroy() {
        try { backgroundCoroutineScope.coroutineContext[Job]?.cancel() } catch (e: Exception) { Log.i("BackgroundCoroutineScope", e.message.orEmpty()) }
        super.onDestroy()
    }

    companion object {

        val TAG: String = MainActivity::class.java.simpleName
        const val APP_INTRO_REQUEST = 100

        private const val LIBRARY = 0
        private const val FOLDERS = 1
        private const val HOME = 2
    }
}
