package player.phonograph.ui.modules.auxiliary

import de.psdev.licensesdialog.LicensesDialog
import player.phonograph.App
import player.phonograph.GITHUB_LINK
import player.phonograph.R
import player.phonograph.TRANSLATE_LINk
import player.phonograph.databinding.ActivityAboutBinding
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.UpdateChecker
import player.phonograph.model.version.VersionCatalog
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.basis.ToolbarActivity
import player.phonograph.ui.dialogs.ChangelogDialog
import player.phonograph.ui.dialogs.DebugDialog
import player.phonograph.ui.dialogs.ReportIssueDialog
import player.phonograph.ui.dialogs.UpgradeInfoDialog
import player.phonograph.util.currentReleaseChannel
import player.phonograph.util.currentVariant
import player.phonograph.util.currentVersionName
import player.phonograph.util.gitRevisionHash
import player.phonograph.util.text.NoticesProcessor
import player.phonograph.util.theme.ThemeSettingsDelegate.isNightTheme
import player.phonograph.util.theme.ThemeSettingsDelegate.primaryColor
import player.phonograph.util.theme.updateSystemBarsColor
import player.phonograph.util.ui.applyWindowInsetsAsBottomView
import util.theme.color.darkenColor
import util.theme.view.toolbar.setToolbarColor
import androidx.annotation.Keep
import androidx.lifecycle.lifecycleScope
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class AboutActivity : ToolbarActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateSystemBarsColor(darkenColor(primaryColor()), Color.TRANSPARENT)

        setUpViews()
    }

    private fun setUpViews() {
        setUpToolbar()
        setUpAppVersion()
        setUpOnClickListeners()

        binding.scrollview.applyWindowInsetsAsBottomView()
    }

    private fun setUpToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setToolbarColor(binding.toolbar, primaryColor())
    }

    @Keep
    @SuppressLint("SetTextI18n")
    private fun setUpAppVersion() {
        val appAboutLayout = binding.activityAboutMainContent.cardAboutAppLayout

        appAboutLayout.appVersion.text = currentVersionName(this)

        val appVersionHash = appAboutLayout.appVersionHash
        try {
            appVersionHash.text = gitRevisionHash(this).substring(0, 8)
            appVersionHash.visibility = View.VISIBLE
        } catch (_: Exception) {
            appVersionHash.visibility = View.INVISIBLE
        }

        val appVariant = appAboutLayout.appVariant
        try {
            val variant = currentVariant()
            appVariant.text = "$variant Variant"
            appVariant.visibility = View.VISIBLE
        } catch (_: Exception) {
            appVariant.visibility = View.GONE
        }
    }

    private fun setUpOnClickListeners() {
        val appAboutLayout = binding.activityAboutMainContent.cardAboutAppLayout
        val originalAuthorLayout = binding.activityAboutMainContent.cardAuthorLayout
        val currentMaintainerLayout = binding.activityAboutMainContent.cardAuthorLayoutModifier
        val supportLayout = binding.activityAboutMainContent.cardSupportDevelopmentLayout
        val specialThanksLayout = binding.activityAboutMainContent.cardSpecialThanksLayout

        appAboutLayout.changelog.setOnClickListener {
            ChangelogDialog.create().show(supportFragmentManager, "CHANGELOG")
        }
        appAboutLayout.checkUpgrade.setOnClickListener {
            checkForUpdates()
        }
        appAboutLayout.licenses.setOnClickListener {
            showLicenseDialog()
        }
        appAboutLayout.forkOnGithub.setOnClickListener {
            openUrl(GITHUB_LINK)
        }
        appAboutLayout.phonographIcon.setOnLongClickListener {
            DebugDialog().show(supportFragmentManager, "DEBUG")
            true
        }

        originalAuthorLayout.writeAnEmail.setOnClickListener {
            sendEmail()
        }
        originalAuthorLayout.followOnTwitter.setOnClickListener {
            openUrl(KABOUZEID_TWITTER)
        }
        originalAuthorLayout.visitWebsite.setOnClickListener {
            openUrl(KABOUZEID_WEBSITE)
        }
        currentMaintainerLayout.github.setOnClickListener {
            openUrl(CHR56_GITHUB)
        }

        supportLayout.intro.setOnClickListener {
            startActivity(Intent(this, PhonographIntroActivity::class.java))
        }
        supportLayout.reportBugs.setOnClickListener {
            ReportIssueDialog().show(supportFragmentManager, "REPORT_ISSUE")
        }
        supportLayout.translate.setOnClickListener {
            openUrl(TRANSLATE_LINk)
        }

        specialThanksLayout.aidanFollestadGitHub.setOnClickListener {
            openUrl(AIDAN_FOLLESTAD_GITHUB)
        }
        specialThanksLayout.michaelCookWebsite.setOnClickListener {
            openUrl(MICHAEL_COOK_WEBSITE)
        }
        specialThanksLayout.maartenCorpelWebsite.setOnClickListener {
            openUrl(MAARTEN_CORPEL_WEBSITE)
        }
        specialThanksLayout.maartenCorpelTwitter.setOnClickListener {
            openUrl(MAARTEN_CORPEL_TWITTER)
        }
        specialThanksLayout.aleksandarTesicTwitter.setOnClickListener {
            openUrl(ALEKSANDAR_TESIC_TWITTER)
        }
        specialThanksLayout.eugeneCheungGitHub.setOnClickListener {
            openUrl(EUGENE_CHEUNG_GITHUB)
        }
        specialThanksLayout.eugeneCheungWebsite.setOnClickListener {
            openUrl(EUGENE_CHEUNG_WEBSITE)
        }
        specialThanksLayout.adrianTwitter.setOnClickListener {
            openUrl(ADRIAN_TWITTER)
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(EMAIL_URI)
            putExtra(Intent.EXTRA_EMAIL, EMAIL_ADDRESS)
            putExtra(Intent.EXTRA_SUBJECT, EMAIL_SUBJECT)
        }
        startActivity(Intent.createChooser(intent, EMAIL_CHOOSER_TITLE))
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            UpdateChecker.checkUpdate { versionCatalog: VersionCatalog, upgradable: Boolean ->
                if (upgradable) {
                    UpgradeInfoDialog.create(versionCatalog).show(supportFragmentManager, "UPGRADE")
                    val ignored = Setting(App.instance)[Keys.ignoreUpgradeDate].data
                    val current = versionCatalog.latest(currentReleaseChannel)?.date ?: 0
                    if (ignored >= current) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            Toast.makeText(this@AboutActivity, R.string.msg_ignored_update, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(this@AboutActivity, R.string.msg_no_updates, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showLicenseDialog() {
        val notices = try {
            NoticesProcessor.readNotices(this)
        } catch (e: Exception) {
            warning(this, "NoticesProcessor", "Failed to read notices", e)
            return
        }
        val nightMode = isNightTheme(resources)
        LicensesDialog.Builder(this).setNotices(notices).setTitle(R.string.label_licenses)
            .setNoticesCssStyle(
                getString(R.string.css_style_license_dialog).replace(
                    "{bg-color}",
                    if (nightMode) "424242" else "ffffff"
                )
                    .replace("{text-color}", if (nightMode) "ffffff" else "000000")
                    .replace("{license-bg-color}", if (nightMode) "535353" else "eeeeee")
            ).setIncludeOwnLicense(true).build().show()
    }

    companion object {
        private const val CHR56_GITHUB = "https://github.com/chr56/"
        private const val KABOUZEID_TWITTER = "https://twitter.com/swiftkarim"
        private const val KABOUZEID_WEBSITE = "https://kabouzeid.com/"
        private const val AIDAN_FOLLESTAD_GITHUB = "https://github.com/afollestad"
        private const val MICHAEL_COOK_WEBSITE = "https://cookicons.co/"
        private const val MAARTEN_CORPEL_WEBSITE = "https://maartencorpel.com/"
        private const val MAARTEN_CORPEL_TWITTER = "https://twitter.com/maartencorpel"
        private const val ALEKSANDAR_TESIC_TWITTER = "https://twitter.com/djsalezmaj"
        private const val EUGENE_CHEUNG_GITHUB = "https://github.com/arkon"
        private const val EUGENE_CHEUNG_WEBSITE = "https://echeung.me/"
        private const val ADRIAN_TWITTER = "https://twitter.com/froschgames"

        private const val EMAIL_ADDRESS = "contact@kabouzeid.com"
        private const val EMAIL_CHOOSER_TITLE = "E-Mail"
        private const val EMAIL_SUBJECT = "Phonograph"
        private const val EMAIL_URI = "mailto:$EMAIL_ADDRESS"
    }
}
