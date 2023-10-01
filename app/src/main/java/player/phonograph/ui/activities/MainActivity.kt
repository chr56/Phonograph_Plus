package player.phonograph.ui.activities

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenDirStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenDirStorageAccessTool
import lib.phonograph.misc.OpenFileStorageAccessTool
import lib.phonograph.misc.Reboot
import mt.tint.viewtint.setItemIconColors
import mt.tint.viewtint.setItemTextColors
import mt.util.color.resolveColor
import mt.util.color.secondaryTextColor
import player.phonograph.BuildConfig
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.UPGRADABLE
import player.phonograph.VERSION_INFO
import player.phonograph.actions.actionPlay
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.coil.loadImage
import player.phonograph.databinding.ActivityMainBinding
import player.phonograph.databinding.LayoutDrawerBinding
import player.phonograph.mechanism.Update
import player.phonograph.mechanism.setting.HomeTabConfig
import player.phonograph.mechanism.setting.StyleConfig
import player.phonograph.model.infoString
import player.phonograph.model.pages.Pages
import player.phonograph.model.version.VersionCatalog
import player.phonograph.notification.UpgradeNotification
import player.phonograph.repo.loader.Songs
import player.phonograph.repo.room.Scanner
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.CurrentQueueState
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.settings.Keys
import player.phonograph.settings.PrerequisiteSetting
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.base.AbsSlidingMusicPanelActivity
import player.phonograph.ui.dialogs.ChangelogDialog
import player.phonograph.ui.dialogs.ScanMediaFolderDialog
import player.phonograph.ui.dialogs.UpgradeDialog
import player.phonograph.ui.fragments.HomeFragment
import player.phonograph.ui.modules.setting.SettingsActivity
import player.phonograph.ui.modules.web.WebSearchLauncher
import player.phonograph.util.currentVersionCode
import player.phonograph.util.debug
import player.phonograph.util.parcelableExtra
import player.phonograph.util.permissions.navigateToAppDetailSetting
import player.phonograph.util.permissions.navigateToStorageSetting
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import player.phonograph.util.warning
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withStarted
import android.content.Intent
import android.os.Build
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
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
                    showUpgradeDialog(intent.parcelableExtra(VERSION_INFO) as? VersionCatalog)
                }
                val setting = Setting(this)
                lifecycleScope.launch(Dispatchers.Main) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                        setting[Keys.homeTabConfigJsonString].flow.distinctUntilChanged().collect {
                            withStarted {
                                setupDrawerMenu(drawerBinding.navigationView.menu)
                            }
                        }
                    }
                }
            }, 900
        )

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                CurrentQueueState.currentSong.collect {
                    updateNavigationDrawerHeader()
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Default) {
            latelySetup()
        }

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

    private fun setupDrawerMenu(menu: Menu) {
        menu.clear()
        attach(this, menu) {
            val activity = this@MainActivity

            // page chooser
            val mainGroup = 999999
            for ((page, tab) in HomeTabConfig.homeTabConfig.withIndex()) {
                menuItem {
                    groupId = mainGroup
                    icon = context.getTintedDrawable(Pages.getTintedIconRes(tab), textColorPrimary)
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
                            val result = StyleConfig.toggleTheme(this@MainActivity)
                            if (!result) {
                                Toast.makeText(activity, R.string.auto_mode_on, Toast.LENGTH_SHORT).show()
                            } else {
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
                            val songs = Songs.all(activity)
                            if (songs.isNotEmpty())
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


            menuItem {
                groupId = groupIds[3]
                icon = getTintedDrawable(R.drawable.ic_more_vert_white_24dp, textColorPrimary)
                titleRes(R.string.more_actions)
                onClick {
                    val items = listOf(
                        context.getString(R.string.refresh_database) to {
                            Scanner.refreshDatabase(context)
                        },
                        context.getString(R.string.refresh_database) to {
                            Scanner.refreshDatabase(context, true)
                        },
                        context.getString(R.string.action_grant_storage_permission) to {
                            navigateToStorageSetting(context)
                        },
                        context.getString(R.string.app_info) to {
                            navigateToAppDetailSetting(context)
                        },
                        context.getString(R.string.action_reboot) to {
                            Reboot.reboot(context)
                        },
                        context.getString(R.string.search_online) to {
                            context.startActivity(WebSearchLauncher.launchIntent(context))
                        },
                    )
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.more_actions)
                        .setItems(items.map { it.first }.toTypedArray()) { dialog, index ->
                            dialog.dismiss()
                            items[index].second.invoke()
                        }
                        .show()
                    true
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
        if (!PrerequisiteSetting.instance(this).introShown) {
            warning(TAG, "Upgrade check was blocked, because AppIntro not shown (auto check requires user opt-in)!")
            return
        }
        lifecycleScope.launch(SupervisorJob()) {
            Update.checkUpdate { versionCatalog: VersionCatalog, upgradable: Boolean ->
                if (upgradable) {
                    val channel = when (BuildConfig.FLAVOR) {
                        "preview" -> "preview"
                        else      -> "stable"
                    }
                    UpgradeNotification.sendUpgradeNotification(versionCatalog, channel)
                }
            }
            Setting(this@MainActivity)[Keys.lastCheckUpgradeTimeStamp].data = System.currentTimeMillis()
        }
    }

    private fun checkChangelog() {
        val currentVersion = currentVersionCode(this)
        val lastChangelogVersion = PrerequisiteSetting.instance(this).lastChangelogVersion

        if (currentVersion > lastChangelogVersion) {
            ChangelogDialog.create().show(supportFragmentManager, "CHANGE_LOG_DIALOG")
        }
    }

    private fun showUpgradeDialog(versionCatalog: VersionCatalog?) {
        versionCatalog?.let {
            UpgradeDialog.create(versionCatalog).show(supportFragmentManager, "UpgradeDialog")
        }
    }


    /**
     * do some non-immediate work here
     */
    private fun latelySetup() {
        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val dynamicShortcutManager = DynamicShortcutManager(this)
            dynamicShortcutManager.initDynamicShortcuts()
            dynamicShortcutManager.updateDynamicShortcuts()
        }
        // check changelog
        checkChangelog()
        // check upgrade
        val setting = Setting(this)
        lifecycleScope.launch {
            setting[Keys.checkUpgradeAtStartup].flow.collect { enabled ->
                if (enabled) {
                    val lastTimeStamp = setting[Keys.lastCheckUpgradeTimeStamp].data
                    val interval = setting.Composites[Keys.checkUpdateInterval].data
                    if (System.currentTimeMillis() > lastTimeStamp + interval.toSeconds() * 1000L) {
                        checkUpdate()
                    } else {
                        debug {
                            Log.v(TAG, "Ignore upgrade check due to CheckUpdateInterval!")
                        }
                    }
                }
            }
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
