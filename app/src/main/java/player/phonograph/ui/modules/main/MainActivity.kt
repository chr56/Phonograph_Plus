package player.phonograph.ui.modules.main

import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.launcher.CreateFileStorageAccessDelegate
import lib.storage.launcher.ICreateFileStorageAccessible
import lib.storage.launcher.IOpenDirStorageAccessible
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDirStorageAccessDelegate
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.R
import player.phonograph.UPGRADABLE
import player.phonograph.VERSION_INFO
import player.phonograph.appshortcuts.DynamicShortcutManager
import player.phonograph.coil.loadImage
import player.phonograph.databinding.ActivityMainBinding
import player.phonograph.databinding.LayoutDrawerBinding
import player.phonograph.mechanism.Update
import player.phonograph.model.Song
import player.phonograph.model.version.VersionCatalog
import player.phonograph.notification.UpgradeNotification
import player.phonograph.settings.Keys
import player.phonograph.settings.PrerequisiteSetting
import player.phonograph.settings.Setting
import player.phonograph.settings.SettingObserver
import player.phonograph.ui.dialogs.ChangelogDialog
import player.phonograph.ui.dialogs.UpgradeInfoDialog
import player.phonograph.ui.modules.explorer.PathSelectorContractTool
import player.phonograph.ui.modules.explorer.PathSelectorRequester
import player.phonograph.ui.modules.panel.AbsSlidingMusicPanelActivity
import player.phonograph.util.currentChannel
import player.phonograph.util.currentVersionCode
import player.phonograph.util.debug
import player.phonograph.util.logMetrics
import player.phonograph.util.observe
import player.phonograph.util.parcelableExtra
import player.phonograph.util.text.infoString
import player.phonograph.util.theme.accentColor
import player.phonograph.util.theme.nightMode
import player.phonograph.util.theme.themeIconColor
import player.phonograph.util.warning
import util.theme.color.primaryTextColor
import util.theme.view.navigationview.setItemIconColors
import util.theme.view.navigationview.setItemTextColors
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : AbsSlidingMusicPanelActivity(),
                     IOpenFileStorageAccessible, ICreateFileStorageAccessible, IOpenDirStorageAccessible,
                     PathSelectorRequester {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var drawerBinding: LayoutDrawerBinding

    private val drawerViewModel: MainDrawerViewModel by viewModels()

    override val createFileStorageAccessDelegate: CreateFileStorageAccessDelegate = CreateFileStorageAccessDelegate()
    override val openDirStorageAccessDelegate: OpenDirStorageAccessDelegate = OpenDirStorageAccessDelegate()
    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()

    override val pathSelectorContractTool: PathSelectorContractTool = PathSelectorContractTool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerActivityResultLauncherDelegate(
            createFileStorageAccessDelegate,
            openDirStorageAccessDelegate,
            openFileStorageAccessDelegate,
            pathSelectorContractTool
        )

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MainFragment.newInstance(), "home")
                .commit()
        }

        setUpDrawer()

        Handler(Looper.getMainLooper()).postDelayed(
            {
                val showUpgradeDialog = intent.getBooleanExtra(UPGRADABLE, false)
                if (showUpgradeDialog) {
                    showUpgradeDialog(intent.parcelableExtra(VERSION_INFO) as? VersionCatalog)
                }
                SettingObserver(this, lifecycleScope).collect(Keys.homeTabConfig, Dispatchers.Main) { pagesConfig ->
                    withStarted {
                        setupDrawerMenu(
                            activity = this@MainActivity,
                            menu = drawerBinding.navigationView.menu,
                            switchPageTo = { drawerViewModel.switchPageTo(it) },
                            closeDrawer = { drawerBinding.drawerLayout.closeDrawers() },
                            pagesConfig = pagesConfig
                        )
                    }
                }
            }, 900
        )

        observe(queueViewModel.currentSong) { song -> updateNavigationDrawerHeader(song) }
        observe(drawerViewModel.selectedPage) { page -> drawerBinding.navigationView.setCheckedItem(1000 + page) }

        lifecycleScope.launch(Dispatchers.Default) { latelySetup() }
        debug { logMetrics("MainActivity.onCreate()") }
    }

    override fun onResume() {
        super.onResume()
        debug { logMetrics("MainActivity.onResume()") }
    }

    override fun createContentView(): View {
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        drawerBinding = LayoutDrawerBinding.inflate(layoutInflater)
        drawerBinding.drawerContentContainer.addView(wrapSlidingMusicPanel(mainBinding.root))

        return drawerBinding.root
    }

    private fun setUpDrawer() {

        // Preparation
        setupDrawerMenu(
            activity = this@MainActivity,
            menu = drawerBinding.navigationView.menu,
            switchPageTo = { drawerViewModel.switchPageTo(it) },
            closeDrawer = { drawerBinding.drawerLayout.closeDrawers() },
            pagesConfig = null
        )

        // color
        val iconColor = themeIconColor(this)
        with(drawerBinding.navigationView) {
            setItemIconColors(iconColor, accentColor())
            setItemTextColors(primaryTextColor(nightMode), accentColor())
        }

        // listener
        drawerBinding.drawerLayout.addDrawerListener(object : SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                onBackPressedDispatcher.addCallback(this@MainActivity, drawerBackPressedCallback)
            }

            override fun onDrawerClosed(drawerView: View) {
                drawerBackPressedCallback.remove()
            }
        })
    }

    private val drawerBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            drawerBinding.drawerLayout.closeDrawers()
        }
    }


    private var navigationDrawerHeader: View? = null
    private fun updateNavigationDrawerHeader(song: Song?) {
        if (song != null) {

            if (navigationDrawerHeader == null) {
                navigationDrawerHeader =
                    drawerBinding.navigationView.inflateHeaderView(R.layout.navigation_drawer_header).also { view ->
                        view.setOnClickListener {
                            drawerBinding.drawerLayout.closeDrawers()
                            requestToExpand()
                        }
                    }
            }

            val navigationDrawerHeader = navigationDrawerHeader
            if (navigationDrawerHeader != null) {
                val title = navigationDrawerHeader.findViewById<TextView>(R.id.title)
                val text = navigationDrawerHeader.findViewById<TextView>(R.id.text)
                val image = navigationDrawerHeader.findViewById<ImageView>(R.id.image)
                title.text = song.title
                text.text = song.infoString()
                loadImage(this)
                    .from(song)
                    .into(
                        onStart = { image.setImageResource(R.drawable.default_album_art) },
                        onSuccess = { image.setImageDrawable(it) }
                    )
                    .enqueue()
            }
        } else {
            if (navigationDrawerHeader != null) {
                drawerBinding.navigationView.removeHeaderView(navigationDrawerHeader!!)
                navigationDrawerHeader = null
            }
        }
    }

    override fun navigateUp() {
        if (drawerBinding.drawerLayout.isDrawerOpen(drawerBinding.navigationView)) {
            drawerBinding.drawerLayout.closeDrawer(drawerBinding.navigationView)
        } else {
            drawerBinding.drawerLayout.openDrawer(drawerBinding.navigationView)
        }
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
                    UpgradeNotification.sendUpgradeNotification(versionCatalog, currentChannel)
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
            UpgradeInfoDialog.create(versionCatalog).show(supportFragmentManager, "UpgradeDialog")
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
                    val interval = setting[Keys.checkUpdateInterval].data
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

        fun launchingIntent(context: Context, flags: Int = 0): Intent =
            Intent(context, MainActivity::class.java).apply {
                if (flags != 0) this.flags = flags
            }

        fun launchingIntent(context: Context, versionCatalog: VersionCatalog, flags: Int = 0): Intent =
            Intent(context, MainActivity::class.java).apply {
                if (flags != 0) this.flags = flags
                this.putExtra(UPGRADABLE, true)
                this.putExtra(VERSION_INFO, versionCatalog)
            }
    }

}
