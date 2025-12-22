package player.phonograph.ui.modules.auxiliary

import de.psdev.licensesdialog.LicensesDialog
import player.phonograph.App
import player.phonograph.R
import player.phonograph.databinding.ActivityAboutBinding
import player.phonograph.foundation.error.warning
import player.phonograph.mechanism.Update
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
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class AboutActivity : ToolbarActivity(), View.OnClickListener {
    private lateinit var binding: ActivityAboutBinding

    private lateinit var appIcon: ImageView
    private lateinit var appVersion: TextView
    private lateinit var appVersionHash: TextView
    private lateinit var appVariant: TextView
    private lateinit var changelog: LinearLayout
    private lateinit var checkUpgrade: LinearLayout
    private lateinit var intro: LinearLayout
    private lateinit var licenses: LinearLayout
    private lateinit var writeAnEmail: LinearLayout
    private lateinit var followOnTwitter: LinearLayout
    private lateinit var forkOnGitHub: LinearLayout
    private lateinit var visitWebsite: LinearLayout
    private lateinit var reportBugs: LinearLayout
    private lateinit var translate: LinearLayout
    private lateinit var cracked: LinearLayout
    private lateinit var aidanFollestadGitHub: AppCompatButton
    private lateinit var michaelCookWebsite: AppCompatButton
    private lateinit var maartenCorpelWebsite: AppCompatButton
    private lateinit var maartenCorpelTwitter: AppCompatButton
    private lateinit var aleksandarTesicTwitter: AppCompatButton
    private lateinit var eugeneCheungGitHub: AppCompatButton
    private lateinit var eugeneCheungWebsite: AppCompatButton
    private lateinit var adrianTwitter: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        binding()

        setContentView(binding.root)
        updateSystemBarsColor(darkenColor(primaryColor()), Color.TRANSPARENT)

        setUpViews()
    }

    private fun binding() {
        appIcon = binding.activityAboutMainContent.cardAboutAppLayout.phonographIcon
        appVersion = binding.activityAboutMainContent.cardAboutAppLayout.appVersion
        appVersionHash = binding.activityAboutMainContent.cardAboutAppLayout.appVersionHash
        appVariant = binding.activityAboutMainContent.cardAboutAppLayout.appVariant
        changelog = binding.activityAboutMainContent.cardAboutAppLayout.changelog
        checkUpgrade = binding.activityAboutMainContent.cardAboutAppLayout.checkUpgrade
        licenses = binding.activityAboutMainContent.cardAboutAppLayout.licenses
        forkOnGitHub = binding.activityAboutMainContent.cardAboutAppLayout.forkOnGithub

        writeAnEmail = binding.activityAboutMainContent.cardAuthorLayout.writeAnEmail
        followOnTwitter = binding.activityAboutMainContent.cardAuthorLayout.followOnTwitter
        visitWebsite = binding.activityAboutMainContent.cardAuthorLayout.visitWebsite

        intro = binding.activityAboutMainContent.cardSupportDevelopmentLayout.intro
        reportBugs = binding.activityAboutMainContent.cardSupportDevelopmentLayout.reportBugs
        translate = binding.activityAboutMainContent.cardSupportDevelopmentLayout.translate
        cracked = binding.activityAboutMainContent.cardSupportDevelopmentLayout.cracked

        aidanFollestadGitHub = binding.activityAboutMainContent.cardSpecialThanksLayout.aidanFollestadGitHub
        michaelCookWebsite = binding.activityAboutMainContent.cardSpecialThanksLayout.michaelCookWebsite
        maartenCorpelTwitter = binding.activityAboutMainContent.cardSpecialThanksLayout.maartenCorpelTwitter
        maartenCorpelWebsite = binding.activityAboutMainContent.cardSpecialThanksLayout.maartenCorpelWebsite
        aleksandarTesicTwitter = binding.activityAboutMainContent.cardSpecialThanksLayout.aleksandarTesicTwitter
        eugeneCheungGitHub = binding.activityAboutMainContent.cardSpecialThanksLayout.eugeneCheungGitHub
        eugeneCheungWebsite = binding.activityAboutMainContent.cardSpecialThanksLayout.eugeneCheungWebsite
        adrianTwitter = binding.activityAboutMainContent.cardSpecialThanksLayout.adrianTwitter
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
        appVersion.text = currentVersionName(this)
        try {
            appVersionHash.text = gitRevisionHash(this).substring(0, 8)
            appVersionHash.visibility = View.VISIBLE
        } catch (e: Exception) {
            appVersionHash.visibility = View.INVISIBLE
        }
        try {
            val variant = currentVariant()
            appVariant.text = "$variant Variant"
        } catch (e: Exception) {
            appVariant.visibility = View.GONE
        }
    }

    private fun setUpOnClickListeners() {

        changelog.setOnClickListener(this)
        checkUpgrade.setOnClickListener(this)
        intro.setOnClickListener(this)
        licenses.setOnClickListener(this)
        followOnTwitter.setOnClickListener(this)
        forkOnGitHub.setOnClickListener(this)
        visitWebsite.setOnClickListener(this)
        reportBugs.setOnClickListener(this)
        translate.setOnClickListener(this)
        writeAnEmail.setOnClickListener(this)
        aidanFollestadGitHub.setOnClickListener(this)
        michaelCookWebsite.setOnClickListener(this)
        maartenCorpelWebsite.setOnClickListener(this)
        maartenCorpelTwitter.setOnClickListener(this)
        aleksandarTesicTwitter.setOnClickListener(this)
        eugeneCheungGitHub.setOnClickListener(this)
        eugeneCheungWebsite.setOnClickListener(this)
        adrianTwitter.setOnClickListener(this)

        appIcon.setOnLongClickListener {
            DebugDialog().show(supportFragmentManager, "DebugDialog")
            return@setOnLongClickListener true
        } // debug Menu

        binding.activityAboutMainContent.cardAuthorLayoutModifier.github.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v) {
            changelog                                                        -> {
                ChangelogDialog.create().show(supportFragmentManager, "CHANGELOG_DIALOG")
            }

            checkUpgrade                                                     -> {
                lifecycleScope.launch(Dispatchers.Unconfined) {
                    Update.checkUpdate { versionCatalog: VersionCatalog, upgradable: Boolean ->
                        if (upgradable) {
                            UpgradeInfoDialog.create(versionCatalog).show(supportFragmentManager, "UPGRADE_DIALOG")
                            val ignored = Setting(App.instance)[Keys.ignoreUpgradeDate].data
                            val current = versionCatalog.latest(currentReleaseChannel)?.date ?: 0
                            if (ignored >= current) {
                                toast(getString(R.string.msg_ignored_update))
                            }
                        } else {
                            toast(getText(R.string.msg_no_updates))
                        }
                    }
                }
            }

            licenses                                                         -> {
                showLicenseDialog()
            }

            intro                                                            -> {
                startActivity(Intent(this, PhonographIntroActivity::class.java))
            }

            followOnTwitter                                                  -> {
                openUrl(TWITTER)
            }

            forkOnGitHub                                                     -> {
                openUrl(GITHUB)
            }

            visitWebsite                                                     -> {
                openUrl(WEBSITE)
            }

            reportBugs                                                       -> {
                ReportIssueDialog().show(supportFragmentManager, "ReportIssueDialog")
            }

            writeAnEmail                                                     -> {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:contact@kabouzeid.com")
                intent.putExtra(Intent.EXTRA_EMAIL, "contact@kabouzeid.com")
                intent.putExtra(Intent.EXTRA_SUBJECT, "Phonograph")
                startActivity(Intent.createChooser(intent, "E-Mail"))
            }

            translate                                                        -> {
                openUrl(TRANSLATE)
            }

            aidanFollestadGitHub                                             -> {
                openUrl(AIDAN_FOLLESTAD_GITHUB)
            }

            michaelCookWebsite                                               -> {
                openUrl(MICHAEL_COOK_WEBSITE)
            }

            maartenCorpelWebsite                                             -> {
                openUrl(MAARTEN_CORPEL_WEBSITE)
            }

            maartenCorpelTwitter                                             -> {
                openUrl(MAARTEN_CORPEL_TWITTER)
            }

            aleksandarTesicTwitter                                           -> {
                openUrl(ALEKSANDAR_TESIC_TWITTER)
            }

            eugeneCheungGitHub                                               -> {
                openUrl(EUGENE_CHEUNG_GITHUB)
            }

            eugeneCheungWebsite                                              -> {
                openUrl(EUGENE_CHEUNG_WEBSITE)
            }

            adrianTwitter                                                    -> {
                openUrl(ADRIAN_TWITTER)
            }

            binding.activityAboutMainContent.cardAuthorLayoutModifier.github -> {
                openUrl(GITHUB_MODIFIER)
            }
        }
    }

    private fun openUrl(url: String) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(url)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    private fun toast(text: CharSequence) {
        Looper.prepare()
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        Looper.loop()
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
        private const val GITHUB = "https://github.com/chr56/Phonograph_Plus"
        private const val TWITTER = "https://twitter.com/swiftkarim"
        private const val WEBSITE = "https://kabouzeid.com/"

        private const val GITHUB_MODIFIER = "https://github.com/chr56/"

        private const val TRANSLATE = "https://crowdin.com/project/phonograph-plus"

        private const val AIDAN_FOLLESTAD_GITHUB = "https://github.com/afollestad"
        private const val MICHAEL_COOK_WEBSITE = "https://cookicons.co/"
        private const val MAARTEN_CORPEL_WEBSITE = "https://maartencorpel.com/"
        private const val MAARTEN_CORPEL_TWITTER = "https://twitter.com/maartencorpel"
        private const val ALEKSANDAR_TESIC_TWITTER = "https://twitter.com/djsalezmaj"
        private const val EUGENE_CHEUNG_GITHUB = "https://github.com/arkon"
        private const val EUGENE_CHEUNG_WEBSITE = "https://echeung.me/"
        private const val ADRIAN_TWITTER = "https://twitter.com/froschgames"
    }
}
