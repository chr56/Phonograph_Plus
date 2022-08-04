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
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import legacy.phonograph.JunkCleaner
import player.phonograph.App
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.UPGRADABLE
import player.phonograph.Updater.checkUpdate
import player.phonograph.VERSION_INFO
import player.phonograph.adapter.PAGERS
import player.phonograph.databinding.ActivityMainBinding
import player.phonograph.databinding.LayoutDrawerBinding
import player.phonograph.dialogs.ChangelogDialog.Companion.create
import player.phonograph.dialogs.ChangelogDialog.Companion.setChangelogRead
import player.phonograph.dialogs.ScanMediaFolderDialog
import player.phonograph.dialogs.UpgradeDialog
import player.phonograph.glide.SongGlideRequest
import player.phonograph.helper.SearchQueryHelper
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.mediastore.PlaylistSongLoader
import player.phonograph.mediastore.SongLoader.getAllSongs
import player.phonograph.misc.SAFCallbackHandlerActivity
import player.phonograph.misc.SafLauncher
import player.phonograph.model.Song
import player.phonograph.notification.ErrorNotification
import player.phonograph.notification.UpgradeNotification
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.activities.intro.AppIntroActivity
import player.phonograph.ui.fragments.mainactivity.AbsMainActivityFragment
import player.phonograph.ui.fragments.mainactivity.home.HomeFragment
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.MusicUtil
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.NavigationViewUtil.setItemIconColors
import util.mddesign.util.NavigationViewUtil.setItemTextColors
import util.mddesign.util.Util as MDthemerUtil

class MainActivity : AbsSlidingMusicPanelActivity(), SAFCallbackHandlerActivity {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var drawerBinding: LayoutDrawerBinding

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
            setBasicFragment(HomeFragment.newInstance())
        } else {
            currentFragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container) as MainActivityFragmentCallbacks
        }

        showIntro()
        Handler(Looper.getMainLooper()).postDelayed({
            if (intent.getBooleanExtra(UPGRADABLE, false)) {
                showUpgradeDialog(intent.getBundleExtra(VERSION_INFO)!!)
            }
            checkUpdate()
            showChangelog()
        }, 900)

        if (DEBUG) {
            Log.v("Metrics", "${System.currentTimeMillis().mod(10000000)} MainActivity.onCreate()")
        }
    }

    override fun onResume() {
        super.onResume()
        if (DEBUG) Log.v(
            "Metrics",
            "${System.currentTimeMillis().mod(10000000)} MainActivity.onResume()"
        )
    }

    override fun createContentView(): View {
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        drawerBinding = LayoutDrawerBinding.inflate(layoutInflater)
        drawerBinding.drawerContentContainer.addView(wrapSlidingMusicPanel(mainBinding.root))

        return drawerBinding.root
    }

    private fun setBasicFragment(fragment: AbsMainActivityFragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, null)
            .commit()
        currentFragment = fragment as MainActivityFragmentCallbacks
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_INTRO_REQUEST) {
            blockRequestPermissions = false
            if (!hasPermissions) {
                requestPermissions()
            }
            create().show(supportFragmentManager, "CHANGE_LOG_DIALOG")
        }
    }

    override fun requestPermissions() {
        if (!blockRequestPermissions) super.requestPermissions()
    }

    private fun setUpDrawer() {
        attach(this, drawerBinding.navigationView.menu) {
            val activity = this@MainActivity

            val groupIds = intArrayOf(0, 1, 2, 3)

            for (tab in Setting.instance.homeTabConfig) {
                menuItem {
                    groupId = groupIds[0]
                    icon = PAGERS.getTintedIcon(tab, textColorPrimary, activity)
                    title = PAGERS.getDisplayName(tab, activity)
                    onClick {
                        Handler(Looper.getMainLooper()).postDelayed({
                            // todo
                        }, 150)
                    }
                }
            }

            menuItem {
                groupId = groupIds[1]
                itemId = R.id.action_shuffle_all
                icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, textColorPrimary)
                titleRes(R.string.action_shuffle_all, activity)
                onClick {
                    Handler(Looper.getMainLooper()).postDelayed({
                        MusicPlayerRemote.openAndShuffleQueue(getAllSongs(activity), true)
                    }, 350)
                }
            }
            menuItem {
                groupId = groupIds[1]
                itemId = R.id.action_scan
                icon = getTintedDrawable(R.drawable.ic_scanner_white_24dp, textColorPrimary)
                titleRes(R.string.scan_media, activity)
                onClick {
                    Handler(Looper.getMainLooper()).postDelayed({
                        ScanMediaFolderDialog().show(supportFragmentManager, "scan_media")
                    }, 200)
                }
            }

            menuItem {
                groupId = groupIds[2]
                itemId = R.id.theme_toggle
                icon = getTintedDrawable(R.drawable.ic_theme_switch_white_24dp, textColorPrimary)
                titleRes(R.string.theme_switch, activity)
                onClick {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val themeSetting = Setting.instance.generalTheme

                        if (themeSetting == R.style.Theme_Phonograph_Auto) {
                            Toast.makeText(activity, R.string.auto_mode_on, Toast.LENGTH_SHORT).show()
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
                }
            }

            menuItem {
                groupId = groupIds[3]
                itemId = R.id.nav_settings
                icon = getTintedDrawable(R.drawable.ic_settings_white_24dp, textColorPrimary)
                titleRes(R.string.action_settings, activity)
                onClick {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent(activity, SettingsActivity::class.java))
                    }, 200)
                }
            }
            menuItem {
                groupId = groupIds[3]
                itemId = R.id.nav_about
                icon = getTintedDrawable(R.drawable.ic_help_white_24dp, textColorPrimary)
                titleRes(R.string.action_about, activity)
                onClick {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent(activity, AboutActivity::class.java))
                    }, 200)
                }
            }

            for (id in groupIds) {
                rootMenu.setGroupEnabled(id, true)
                rootMenu.setGroupCheckable(id, false, false)
            }
        }

        with(drawerBinding.drawerLayout) {
            setPadding(
                paddingLeft,
                paddingTop + mainBinding.statusBarLayout.statusBar.height,
                paddingRight,
                paddingBottom
            )
        }

        val iconColor =
            MDthemerUtil.resolveColor(this, R.attr.iconColor, ThemeColor.textColorSecondary(this))

        with(drawerBinding.navigationView) {
            setItemIconColors(this, iconColor, accentColor)
            setItemTextColors(this, textColorPrimary, accentColor)
        }

        drawerBinding.navigationView.setNavigationItemSelectedListener {
            drawerBinding.drawerLayout.closeDrawers()
            true
        }
    }

    private fun updateNavigationDrawerHeader() {
        if (MusicPlayerRemote.playingQueue.isNotEmpty()) {
            val song = MusicPlayerRemote.currentSong

            if (navigationDrawerHeader == null) {
                navigationDrawerHeader =
                    drawerBinding.navigationView.inflateHeaderView(
                        R.layout.navigation_drawer_header
                    )
                (navigationDrawerHeader as View).setOnClickListener {
                    drawerBinding.drawerLayout.closeDrawers()
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
                drawerBinding.navigationView.removeHeaderView(navigationDrawerHeader!!)
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
            if (drawerBinding.drawerLayout.isDrawerOpen(drawerBinding.navigationView)) {
                drawerBinding.drawerLayout.closeDrawer(drawerBinding.navigationView)
            } else {
                drawerBinding.drawerLayout.openDrawer(drawerBinding.navigationView)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun handleBackPress(): Boolean {
        if (drawerBinding.drawerLayout.isDrawerOpen(drawerBinding.navigationView)) {
            drawerBinding.drawerLayout.closeDrawers()
            return true
        }
        return super.handleBackPress() || currentFragment.handleBackPress()
    }

    private fun handlePlaybackIntent(intent: Intent) {
        var handled = false

        intent.action?.let {
            if (it == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
                val songs = SearchQueryHelper.getSongs(this, intent.extras!!)
                if (MusicPlayerRemote.shuffleMode == ShuffleMode.SHUFFLE) {
                    MusicPlayerRemote.openAndShuffleQueue(songs, true)
                } else {
                    if (Setting.instance.keepPlayingQueueIntact) {
                        MusicPlayerRemote.playNow(songs)
                    } else {
                        MusicPlayerRemote.openQueue(songs, 0, true)
                    }
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
                        if (Setting.instance.keepPlayingQueueIntact) {
                            MusicPlayerRemote.playNow(songs)
                        } else {
                            MusicPlayerRemote.openQueue(songs, 0, true)
                        }
                        handled = true
                    }
                }
                MediaStore.Audio.Albums.CONTENT_TYPE -> {
                    val id = parseIdFromIntent(intent, "albumId", "album")
                    if (id >= 0) {
                        val position = intent.getIntExtra("position", 0)
                        val songs = AlbumLoader.getAlbum(this, id).songs
                        if (Setting.instance.keepPlayingQueueIntact) {
                            MusicPlayerRemote.playNow(songs)
                        } else {
                            MusicPlayerRemote.openQueue(songs, 0, true)
                        }
                        handled = true
                    }
                }
                MediaStore.Audio.Artists.CONTENT_TYPE -> {
                    val id = parseIdFromIntent(intent, "artistId", "artist")
                    if (id >= 0) {
                        val position = intent.getIntExtra("position", 0)
                        val songs = ArtistLoader.getArtist(this, id).songs
                        if (Setting.instance.keepPlayingQueueIntact) {
                            MusicPlayerRemote.playNow(songs)
                        } else {
                            MusicPlayerRemote.openQueue(songs, 0, true)
                        }

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
        drawerBinding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onPanelCollapsed(panel: View?) {
        super.onPanelCollapsed(panel)
        drawerBinding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private fun showIntro() {
        if (!Setting.instance.introShown) {
            Setting.instance.introShown = true
            setChangelogRead(this)

            blockRequestPermissions = true

            Handler(Looper.getMainLooper()).postDelayed({
                startActivityForResult(
                    Intent(this@MainActivity, AppIntroActivity::class.java),
                    APP_INTRO_REQUEST
                )
            }, 50)
        }
    }

    private fun checkUpdate() {
        checkUpdate(callback = { UpgradeNotification.sendUpgradeNotification(it) })
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
            ErrorNotification.postErrorNotification(e, "Package Name Can't Be Found!")
        }
    }

    private fun showUpgradeDialog(versionInfo: Bundle) {
        UpgradeDialog.create(versionInfo).show(supportFragmentManager, "UpgradeDialog")
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val APP_INTRO_REQUEST = 100
    }

    interface MainActivityFragmentCallbacks {
        fun handleBackPress(): Boolean
    }
}
