package player.phonograph.ui.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import legacy.phonograph.JunkCleaner
import player.phonograph.*
import player.phonograph.Updater.checkUpdate
import player.phonograph.databinding.*
import player.phonograph.dialogs.ChangelogDialog.Companion.create
import player.phonograph.dialogs.ChangelogDialog.Companion.setChangelogRead
import player.phonograph.dialogs.ScanMediaFolderDialog
import player.phonograph.dialogs.UpgradeDialog
import player.phonograph.glide.SongGlideRequest
import player.phonograph.helper.SearchQueryHelper
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.mediastore.PlaylistSongLoader
import player.phonograph.mediastore.SongLoader
import player.phonograph.misc.SAFCallbackHandlerActivity
import player.phonograph.misc.SafLauncher
import player.phonograph.model.Song
import player.phonograph.notification.UpgradeNotification
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.MusicService
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.activities.intro.AppIntroActivity
import player.phonograph.ui.fragments.mainactivity.AbsMainActivityFragment
import player.phonograph.ui.fragments.mainactivity.folders.FoldersFragment
import player.phonograph.ui.fragments.mainactivity.home.HomeFragment
import player.phonograph.util.MusicUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.NavigationViewUtil
import util.mddesign.util.Util as MDthemerUtil

class MainActivity : AbsSlidingMusicPanelActivity(), SAFCallbackHandlerActivity {
    var activityMainPageBinding: ActivityMainPageBinding? = null
    val pageBinding get() = activityMainPageBinding!!

    var activityMainBinding: ActivityMainBinding? = null
    val mainBinding get() = activityMainBinding!!

    private lateinit var currentFragment: MainActivityFragmentCallbacks
    private var navigationDrawerHeader: View? = null
    private var blockRequestPermissions = false

    private lateinit var safLauncher: SafLauncher
    override fun getSafLauncher(): SafLauncher = safLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        safLauncher = SafLauncher(activityResultRegistry)
        lifecycle.addObserver(safLauncher)

        setUpDrawer()

        if (savedInstanceState == null) {
            setMusicChooser(Setting.instance.lastMusicChooser)
        } else {
            currentFragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container) as MainActivityFragmentCallbacks
        }

        Log.d("MainActivity", "StartIntent:$intent: UPGRADABLE-${intent.getBooleanExtra(UPGRADABLE, false)}")
        if (intent.getBooleanExtra(UPGRADABLE, false)) {
            Log.d("Updater", "receive upgradable notification intent!")
            showUpgradeDialog(intent.getBundleExtra(VERSION_INFO)!!)
        }

        showIntro()
        checkUpdate()
        showChangelog()
    }

    override fun onResume() {
        super.onResume()
        if (BuildConfig.DEBUG) Log.v("Metrics", "${System.currentTimeMillis().mod(10000000)} MainActivity.onResume()")
    }

    override fun createContentView(): View {

        activityMainPageBinding = ActivityMainPageBinding.inflate(layoutInflater)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)

        mainBinding.drawerContentContainer.addView(wrapSlidingMusicPanel(pageBinding.root))

        return mainBinding.root
    }

    override fun onDestroy() {
        activityMainPageBinding = null
        activityMainBinding = null
        super.onDestroy()
    }

    private fun setMusicChooser(key: Int) {
        Setting.instance.lastMusicChooser = key
        when (key) {
            FOLDERS -> {
                mainBinding.navigationView.setCheckedItem(R.id.nav_folders)
                setCurrentFragment(FoldersFragment.newInstance())
            }
            HOME -> {
                mainBinding.navigationView.setCheckedItem(R.id.nav_home)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_INTRO_REQUEST) {
            blockRequestPermissions = false
            if (!hasPermissions()) {
                requestPermissions()
            }
            create().show(supportFragmentManager, "CHANGE_LOG_DIALOG")
        }
    }

    override fun requestPermissions() {
        if (!blockRequestPermissions) super.requestPermissions()
    }

    private fun setUpDrawer() {
        val accentColor = ThemeColor.accentColor(this)
        NavigationViewUtil.setItemIconColors(
            mainBinding.navigationView, MDthemerUtil.resolveColor(this, R.attr.iconColor, ThemeColor.textColorSecondary(this)), accentColor
        )
        NavigationViewUtil.setItemTextColors(
            mainBinding.navigationView, ThemeColor.textColorPrimary(this), accentColor
        )

        mainBinding.navigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            mainBinding.drawerLayout.closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_folders -> Handler().postDelayed({ setMusicChooser(FOLDERS) }, 200)
                R.id.nav_home -> Handler().postDelayed({ setMusicChooser(HOME) }, 200)

                R.id.action_shuffle_all -> Handler().postDelayed({
                    MusicPlayerRemote.openAndShuffleQueue(SongLoader.getAllSongs(this), true)
                }, 350)
                R.id.action_scan -> Handler().postDelayed({
                    ScanMediaFolderDialog().show(supportFragmentManager, "SCAN_MEDIA_FOLDER_CHOOSER")
                }, 200)
                R.id.theme_toggle -> Handler().postDelayed({
                    val themeSetting = Setting.instance.generalTheme

                    if (themeSetting == R.style.Theme_Phonograph_Auto) {
                        Toast.makeText(this, R.string.auto_mode_on, Toast.LENGTH_SHORT).show()
                    } else {
                        when (themeSetting) {
                            R.style.Theme_Phonograph_Light ->
                                Setting.instance.setGeneralTheme("dark")
                            R.style.Theme_Phonograph_Dark, R.style.Theme_Phonograph_Black ->
                                Setting.instance.setGeneralTheme("light")
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
        if (MusicPlayerRemote.playingQueue.isNotEmpty()) {
            val song = MusicPlayerRemote.currentSong

            if (navigationDrawerHeader == null) {
                navigationDrawerHeader =
                    mainBinding.navigationView.inflateHeaderView(R.layout.navigation_drawer_header)
                (navigationDrawerHeader as View).setOnClickListener {
                    mainBinding.drawerLayout.closeDrawers()
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
                mainBinding.navigationView.removeHeaderView(navigationDrawerHeader!!)
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
            if (mainBinding.drawerLayout.isDrawerOpen(mainBinding.navigationView)) {
                mainBinding.drawerLayout.closeDrawer(mainBinding.navigationView)
            } else {
                mainBinding.drawerLayout.openDrawer(mainBinding.navigationView)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun handleBackPress(): Boolean {
        if (mainBinding.drawerLayout.isDrawerOpen(mainBinding.navigationView)) {
            mainBinding.drawerLayout.closeDrawers()
            return true
        }
        return super.handleBackPress() || currentFragment.handleBackPress()
    }

    private fun handlePlaybackIntent(intent: Intent) {
        var handled = false

        intent.action?.let {
            if (it == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
                val songs = SearchQueryHelper.getSongs(this, intent.extras!!)
                if (MusicPlayerRemote.shuffleMode == MusicService.SHUFFLE_MODE_SHUFFLE) {
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

    override fun onPanelExpanded(panel: View?) {
        super.onPanelExpanded(panel)
        mainBinding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onPanelCollapsed(panel: View?) {
        super.onPanelCollapsed(panel)
        mainBinding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private fun showIntro() {
        if (!Setting.instance.introShown) {
            Setting.instance.introShown = true
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
            if (currentVersion != Setting.instance.lastChangeLogVersion) {
                create().show(supportFragmentManager, "CHANGE_LOG_DIALOG")
                JunkCleaner(App.instance).clear(currentVersion, CoroutineScope(Dispatchers.IO))
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
    }

    companion object {

        const val TAG = "MainActivity"
        const val APP_INTRO_REQUEST = 100

        private const val HOME = 0
        private const val FOLDERS = 1
    }
}
