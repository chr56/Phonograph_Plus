package player.phonograph.ui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import legacy.phonograph.JunkCleaner
import mt.tint.viewtint.setItemIconColors
import mt.tint.viewtint.setItemTextColors
import mt.util.color.resolveColor
import mt.util.color.secondaryTextColor
import player.phonograph.App
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.UPGRADABLE
import player.phonograph.util.UpdateUtil.checkUpdate
import player.phonograph.VERSION_INFO
import player.phonograph.coil.loadImage
import player.phonograph.databinding.ActivityMainBinding
import player.phonograph.databinding.LayoutDrawerBinding
import player.phonograph.dialogs.ChangelogDialog
import player.phonograph.dialogs.ChangelogDialog.Companion.setChangelogRead
import player.phonograph.ui.dialogs.ScanMediaFolderDialog
import player.phonograph.dialogs.UpgradeDialog
import player.phonograph.helper.SearchQueryHelper
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.mediastore.PlaylistSongLoader
import player.phonograph.mediastore.SongLoader.getAllSongs
import player.phonograph.misc.SAFCallbackHandlerActivity
import player.phonograph.misc.SafLauncher
import player.phonograph.model.infoString
import player.phonograph.model.pages.Pages
import player.phonograph.notification.ErrorNotification
import player.phonograph.notification.UpgradeNotification
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.util.preferences.HomeTabConfig
import player.phonograph.settings.Setting
import player.phonograph.util.preferences.StyleConfig
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.activities.intro.AppIntroActivity
import player.phonograph.ui.fragments.HomeFragment
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PhonographColorUtil.nightMode

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

        currentFragment =
            if (savedInstanceState == null) {
                HomeFragment.newInstance().apply {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, this, "home")
                        .commit()
                }
            } else {
                supportFragmentManager.findFragmentById(R.id.fragment_container) as MainActivityFragmentCallbacks
            }

        setUpDrawer()

        showIntro()
        Handler(Looper.getMainLooper()).postDelayed({
            val showUpgradeDialog = intent.getBooleanExtra(SHOW_UPGRADE_DIALOG, false)
            if (showUpgradeDialog) {
                showUpgradeDialog(intent.getBundleExtra(VERSION_INFO)!!)
            } else {
                checkUpdate()
            }
            showChangelog()
            Setting.instance.registerOnSharedPreferenceChangedListener(
                sharedPreferenceChangeListener
            )
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

    override fun onDestroy() {
        super.onDestroy()
        Setting.instance.unregisterOnSharedPreferenceChangedListener(sharedPreferenceChangeListener)
    }

    override fun createContentView(): View {
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        drawerBinding = LayoutDrawerBinding.inflate(layoutInflater)
        drawerBinding.drawerContentContainer.addView(wrapSlidingMusicPanel(mainBinding.root))

        return drawerBinding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_INTRO_REQUEST) {
            blockRequestPermissions = false
            if (!hasPermissions) {
                requestPermissions()
            }
            ChangelogDialog.create().show(supportFragmentManager, "CHANGE_LOG_DIALOG")
        }
    }

    override fun requestPermissions() {
        if (!blockRequestPermissions) super.requestPermissions()
    }

    private fun inflateDrawerMenu(menu: Menu) {
        attach(this, menu) {
            val activity = this@MainActivity

            // page chooser
            val mainGroup = 999999
            for ((page, tab) in HomeTabConfig.homeTabConfig.withIndex()) {
                menuItem {
                    groupId = mainGroup
                    icon = Pages.getTintedIcon(tab, textColorPrimary, activity)
                    title = Pages.getDisplayName(tab, activity)
                    itemId = 1000 + page
                    onClick {
                        drawerBinding.drawerLayout.closeDrawers()
                        Handler(Looper.getMainLooper()).postDelayed({
                            currentFragment.requestSelectPage(page)
                        }, 150)
                    }
                }
            }

            // normal items
            val groupIds = intArrayOf(0, 1, 2, 3)
            menuItem {
                groupId = groupIds[1]
                itemId = R.id.action_theme_toggle
                icon = getTintedDrawable(R.drawable.ic_theme_switch_white_24dp, textColorPrimary)
                titleRes(R.string.theme_switch)
                onClick {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val themeSetting = StyleConfig.generalTheme

                        if (themeSetting == R.style.Theme_Phonograph_Auto) {
                            Toast.makeText(activity, R.string.auto_mode_on, Toast.LENGTH_SHORT).show()
                        } else {
                            when (themeSetting) {
                                R.style.Theme_Phonograph_Light ->
                                    StyleConfig.setGeneralTheme("dark")
                                R.style.Theme_Phonograph_Dark, R.style.Theme_Phonograph_Black ->
                                    StyleConfig.setGeneralTheme("light")
                            }
                            recreate()
                        }
                    }, 200)
                }
            }

            menuItem {
                groupId = groupIds[2]
                itemId = R.id.action_shuffle_all
                icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, textColorPrimary)
                titleRes(R.string.action_shuffle_all)
                onClick {
                    drawerBinding.drawerLayout.closeDrawers()
                    Handler(Looper.getMainLooper()).postDelayed({
                        val songs = getAllSongs(activity)
                        MusicPlayerRemote
                            .playQueue(songs, 0, true, ShuffleMode.SHUFFLE)
                    }, 350)
                }
            }
            menuItem {
                groupId = groupIds[2]
                itemId = R.id.action_scan
                icon = getTintedDrawable(R.drawable.ic_scanner_white_24dp, textColorPrimary)
                titleRes(R.string.scan_media)
                onClick {
                    drawerBinding.drawerLayout.closeDrawers()
                    Handler(Looper.getMainLooper()).postDelayed({
                        ScanMediaFolderDialog().show(supportFragmentManager, "scan_media")
                    }, 200)
                }
            }

            menuItem {
                groupId = groupIds[3]
                itemId = R.id.nav_settings
                icon = getTintedDrawable(R.drawable.ic_settings_white_24dp, textColorPrimary)
                titleRes(R.string.action_settings)
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
                titleRes(R.string.action_about)
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
            rootMenu.setGroupCheckable(mainGroup, true, true)
        }
    }

    private fun setUpDrawer() {
        // inflate & setup drawer menu item
        inflateDrawerMenu(drawerBinding.navigationView.menu)

        // padding
        with(drawerBinding.drawerLayout) {
            setPadding(
                paddingLeft,
                paddingTop + mainBinding.statusBarLayout.statusBar.height,
                paddingRight,
                paddingBottom
            )
        }

        // color
        val iconColor =
            resolveColor(this, R.attr.iconColor, secondaryTextColor(resources.nightMode))
        with(drawerBinding.navigationView) {
            setItemIconColors(iconColor, accentColor)
            setItemTextColors(textColorPrimary, accentColor)
        }
    }

    fun switchPageChooserTo(page: Int) {
        drawerBinding.navigationView.setCheckedItem(1000 + page)
    }

    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            Setting.HOME_TAB_CONFIG -> {
                with(drawerBinding.navigationView.menu) {
                    clear()
                    inflateDrawerMenu(this)
                }
            }
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
                song.infoString()
            val image = navigationDrawerHeader!!.findViewById<ImageView>(R.id.image)
            loadImage(this) {
                data(song)
                target(
                    onStart = { image.setImageResource(R.drawable.default_album_art) },
                    onSuccess = { image.setImageDrawable(it) }
                )
            }
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
                MusicPlayerRemote.playQueueCautiously(songs, 0, true, null)
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
                        val songs = PlaylistSongLoader.getPlaylistSongList(this, id)
                        MusicPlayerRemote.playQueueCautiously(songs, position, true, null)
                        handled = true
                    }
                }
                MediaStore.Audio.Albums.CONTENT_TYPE -> {
                    val id = parseIdFromIntent(intent, "albumId", "album")
                    if (id >= 0) {
                        val position = intent.getIntExtra("position", 0)
                        val songs = AlbumLoader.getAlbum(this, id).songs
                        MusicPlayerRemote.playQueueCautiously(songs, position, true, null)
                        handled = true
                    }
                }
                MediaStore.Audio.Artists.CONTENT_TYPE -> {
                    val id = parseIdFromIntent(intent, "artistId", "artist")
                    if (id >= 0) {
                        val position = intent.getIntExtra("position", 0)
                        val songs = ArtistLoader.getArtist(this, id).songs
                        MusicPlayerRemote.playQueueCautiously(songs, position, true, null)
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
        CoroutineScope(SupervisorJob()).launch {
            checkUpdate(false)?.let {
                if (it.getBoolean(UPGRADABLE))
                    UpgradeNotification.sendUpgradeNotification(it)
            }
        }
    }

    private fun showChangelog() {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersion = pInfo.versionCode
            if (currentVersion != Setting.instance.lastChangeLogVersion) {
                runCatching {
                    ChangelogDialog.create().show(supportFragmentManager, "CHANGE_LOG_DIALOG")
                    JunkCleaner(App.instance).clear(currentVersion, CoroutineScope(Dispatchers.IO))
                }
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

        const val SHOW_UPGRADE_DIALOG = "show_upgrade_dialog"
    }

    interface MainActivityFragmentCallbacks {
        fun handleBackPress(): Boolean
        fun requestSelectPage(page: Int)
    }
}
