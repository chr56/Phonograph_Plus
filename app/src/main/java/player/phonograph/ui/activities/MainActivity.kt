package player.phonograph.ui.activities

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import mt.tint.viewtint.setItemIconColors
import mt.tint.viewtint.setItemTextColors
import mt.util.color.resolveColor
import mt.util.color.secondaryTextColor
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.UPGRADABLE
import player.phonograph.VERSION_INFO
import player.phonograph.actions.actionPlay
import player.phonograph.coil.loadImage
import player.phonograph.databinding.ActivityMainBinding
import player.phonograph.databinding.LayoutDrawerBinding
import player.phonograph.dialogs.ChangelogDialog
import player.phonograph.mediastore.SongLoader.getAllSongs
import player.phonograph.migrate.migrate
import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenDirStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenDirStorageAccessTool
import lib.phonograph.misc.OpenFileStorageAccessTool
import player.phonograph.model.infoString
import player.phonograph.model.pages.Pages
import player.phonograph.model.version.VersionCatalog
import player.phonograph.notification.ErrorNotification
import player.phonograph.notification.UpgradeNotification
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.dialogs.ScanMediaFolderDialog
import player.phonograph.ui.dialogs.UpgradeDialog
import player.phonograph.ui.fragments.HomeFragment
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.UpdateUtil
import player.phonograph.util.Util.debug
import player.phonograph.util.Util.warning
import player.phonograph.util.preferences.HomeTabConfig
import player.phonograph.util.preferences.StyleConfig
import androidx.drawerlayout.widget.DrawerLayout
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : AbsSlidingMusicPanelActivity(),
                     IOpenFileStorageAccess, ICreateFileStorageAccess, IOpenDirStorageAccess {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var drawerBinding: LayoutDrawerBinding

    private lateinit var currentFragment: MainActivityFragmentCallbacks
    private var navigationDrawerHeader: View? = null

    override val openFileStorageAccessTool: OpenFileStorageAccessTool =
        OpenFileStorageAccessTool()
    override val openDirStorageAccessTool: OpenDirStorageAccessTool =
        OpenDirStorageAccessTool()
    override val createFileStorageAccessTool: CreateFileStorageAccessTool =
        CreateFileStorageAccessTool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        openDirStorageAccessTool.register(lifecycle, activityResultRegistry)
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)

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

        Handler(Looper.getMainLooper()).postDelayed(
            {
                val showUpgradeDialog = intent.getBooleanExtra(UPGRADABLE, false)
                if (showUpgradeDialog) {
                    showUpgradeDialog(intent.getParcelableExtra(VERSION_INFO) as? VersionCatalog)
                } else {
                    checkUpdate()
                }
                versionCheck()
                Setting.instance.observe(
                    this,
                    arrayOf(Setting.HOME_TAB_CONFIG)
                ) { _, key ->
                    if (key == Setting.HOME_TAB_CONFIG)
                        with(drawerBinding.navigationView.menu) {
                            clear()
                            inflateDrawerMenu(this)
                        }
                }
            }, 900
        )

        if (DEBUG) {
            Log.v("Metrics", "${System.currentTimeMillis().mod(10000000)} MainActivity.onCreate()")
        }
    }

    override fun onResume() {
        super.onResume()
        debug {
            Log.v(
                "Metrics",
                "${System.currentTimeMillis().mod(10000000)} MainActivity.onResume()"
            )
        }
    }

    override fun createContentView(): View {
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        drawerBinding = LayoutDrawerBinding.inflate(layoutInflater)
        drawerBinding.drawerContentContainer.addView(wrapSlidingMusicPanel(mainBinding.root))

        return drawerBinding.root
    }

    override fun requestPermissions() {} // not allow

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
                        Handler(Looper.getMainLooper()).postDelayed(
                            { currentFragment.requestSelectPage(page) }, 150
                        )
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
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            val themeSetting = StyleConfig.generalTheme(this@MainActivity)
                            if (themeSetting == R.style.Theme_Phonograph_Auto) {
                                Toast.makeText(
                                    activity, R.string.auto_mode_on, Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                when (themeSetting) {
                                    R.style.Theme_Phonograph_Light ->
                                        StyleConfig.setGeneralTheme("dark")
                                    R.style.Theme_Phonograph_Dark, R.style.Theme_Phonograph_Black ->
                                        StyleConfig.setGeneralTheme("light")
                                }
                                recreate()
                            }
                        }, 200
                    )
                }
            }

            menuItem {
                groupId = groupIds[2]
                itemId = R.id.action_shuffle_all
                icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, textColorPrimary)
                titleRes(R.string.action_shuffle_all)
                onClick {
                    drawerBinding.drawerLayout.closeDrawers()
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            val songs = getAllSongs(activity)
                            songs.actionPlay(ShuffleMode.SHUFFLE, Random.nextInt(songs.size))
                        }, 350
                    )
                }
            }
            menuItem {
                groupId = groupIds[2]
                itemId = R.id.action_scan
                icon = getTintedDrawable(R.drawable.ic_scanner_white_24dp, textColorPrimary)
                titleRes(R.string.scan_media)
                onClick {
                    drawerBinding.drawerLayout.closeDrawers()
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            ScanMediaFolderDialog().show(supportFragmentManager, "scan_media")
                        }, 200
                    )
                }
            }

            menuItem {
                groupId = groupIds[3]
                itemId = R.id.nav_settings
                icon = getTintedDrawable(R.drawable.ic_settings_white_24dp, textColorPrimary)
                titleRes(R.string.action_settings)
                onClick {
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            startActivity(
                                Intent(activity, SettingsActivity::class.java)
                            )
                        }, 200
                    )
                }
            }
            menuItem {
                groupId = groupIds[3]
                itemId = R.id.nav_about
                icon = getTintedDrawable(R.drawable.ic_help_white_24dp, textColorPrimary)
                titleRes(R.string.action_about)
                onClick {
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            startActivity(
                                Intent(activity, AboutActivity::class.java)
                            )
                        }, 200
                    )
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
            resolveColor(this, R.attr.iconColor, secondaryTextColor(nightMode))
        with(drawerBinding.navigationView) {
            setItemIconColors(iconColor, accentColor)
            setItemTextColors(textColorPrimary, accentColor)
        }
    }

    fun switchPageChooserTo(page: Int) {
        drawerBinding.navigationView.setCheckedItem(1000 + page)
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
    }

    override fun navigateUp() {
        if (drawerBinding.drawerLayout.isDrawerOpen(drawerBinding.navigationView)) {
            drawerBinding.drawerLayout.closeDrawer(drawerBinding.navigationView)
        } else {
            drawerBinding.drawerLayout.openDrawer(drawerBinding.navigationView)
        }
    }

    override fun handleBackPress(): Boolean {
        if (drawerBinding.drawerLayout.isDrawerOpen(drawerBinding.navigationView)) {
            drawerBinding.drawerLayout.closeDrawers()
            return true
        }
        return super.handleBackPress() || currentFragment.handleBackPress()
    }

    override fun onPanelExpanded(panel: View?) {
        super.onPanelExpanded(panel)
        drawerBinding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun onPanelCollapsed(panel: View?) {
        super.onPanelCollapsed(panel)
        drawerBinding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private fun checkUpdate() {
        if (!Setting.instance.checkUpgradeAtStartup) return
        if (!Setting.instance.introShown) {
            warning(TAG, "Upgrade check was blocked, because AppIntro not shown (auto check requires user opt-in)!")
            return
        }
        CoroutineScope(SupervisorJob()).launch {
            UpdateUtil.checkUpdate { versionCatalog: VersionCatalog, upgradable: Boolean ->
                if (upgradable) {
                    val channel = when (BuildConfig.FLAVOR) {
                        "preview" -> "preview"
                        else      -> "stable"
                    }
                    UpgradeNotification.sendUpgradeNotification(versionCatalog, channel)
                }
            }
        }
    }

    private fun versionCheck() {
        try {
            val currentVersion = packageManager.getPackageInfo(packageName, 0).versionCode
            val previousVersion = Setting.instance.previousVersion

            if (currentVersion > previousVersion) {
                ChangelogDialog.create().show(supportFragmentManager, "CHANGE_LOG_DIALOG")
            }

            CoroutineScope(Dispatchers.Default).launch {
                migrate(App.instance, previousVersion, currentVersion)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            ErrorNotification.postErrorNotification(e, "Package Name Can't Be Found!")
        }
    }

    private fun showUpgradeDialog(versionCatalog: VersionCatalog?) {
        versionCatalog?.let {
            UpgradeDialog.create(versionCatalog).show(supportFragmentManager, "UpgradeDialog")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    interface MainActivityFragmentCallbacks {
        fun handleBackPress(): Boolean
        fun requestSelectPage(page: Int)
    }
}
