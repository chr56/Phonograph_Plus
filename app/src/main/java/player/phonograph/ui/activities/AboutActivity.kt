package player.phonograph.ui.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import de.psdev.licensesdialog.LicensesDialog
import lib.phonograph.activity.ToolbarActivity
import player.phonograph.App.Companion.instance
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.Updater
import player.phonograph.databinding.ActivityAboutBinding
import player.phonograph.dialogs.ChangelogDialog
import player.phonograph.dialogs.DebugDialog
import player.phonograph.dialogs.UpgradeDialog
import player.phonograph.settings.Setting
import player.phonograph.ui.activities.bugreport.BugReportActivity
import player.phonograph.ui.activities.intro.AppIntroActivity
import util.mdcolor.pref.ThemeColor
import util.mddesign.core.Themer.setActivityToolbarColorAuto

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class AboutActivity : ToolbarActivity(), View.OnClickListener {
    private lateinit var binding: ActivityAboutBinding

    private lateinit var mToolbar: Toolbar
    private lateinit var appIcon: ImageView
    private lateinit var appVersion: TextView
    private lateinit var appVersionHash: TextView
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
        mToolbar = binding.toolbar

        setActivityToolbarColorAuto(this, mToolbar)

        setContentView(binding.root)

        setDrawUnderStatusbar()

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        setUpViews()
    }

    private fun binding() {
        appIcon = binding.activityAboutMainContent.cardAboutAppLayout.phonographIcon
        appVersion = binding.activityAboutMainContent.cardAboutAppLayout.appVersion
        appVersionHash = binding.activityAboutMainContent.cardAboutAppLayout.appVersionHash
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

        aidanFollestadGitHub =
            binding.activityAboutMainContent.cardSpecialThanksLayout.aidanFollestadGitHub
        michaelCookWebsite =
            binding.activityAboutMainContent.cardSpecialThanksLayout.michaelCookWebsite
        maartenCorpelTwitter =
            binding.activityAboutMainContent.cardSpecialThanksLayout.maartenCorpelTwitter
        maartenCorpelWebsite =
            binding.activityAboutMainContent.cardSpecialThanksLayout.maartenCorpelWebsite
        aleksandarTesicTwitter =
            binding.activityAboutMainContent.cardSpecialThanksLayout.aleksandarTesicTwitter
        eugeneCheungGitHub =
            binding.activityAboutMainContent.cardSpecialThanksLayout.eugeneCheungGitHub
        eugeneCheungWebsite =
            binding.activityAboutMainContent.cardSpecialThanksLayout.eugeneCheungWebsite
        adrianTwitter = binding.activityAboutMainContent.cardSpecialThanksLayout.adrianTwitter
    }

    private fun setUpViews() {
        setUpToolbar()
        setUpAppVersion()
        setUpOnClickListeners()
    }

    private fun setUpToolbar() {
        mToolbar.setBackgroundColor(ThemeColor.primaryColor(this))
        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    @Keep
    private fun setUpAppVersion() {
        appVersion.text = getCurrentVersionName(this)
        try {
            appVersionHash.text = BuildConfig.GIT_COMMIT_HASH.substring(0, 8)
            appVersionHash.visibility = View.VISIBLE
        } catch (e: Exception) {
            appVersionHash.visibility = View.INVISIBLE
        }
    }
    private fun getCurrentVersionName(context: Context): String {
        try {
            return context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return "Unknown"
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(v: View) {
        when (v) {
            changelog -> {
                ChangelogDialog.create().show(supportFragmentManager, "CHANGELOG_DIALOG")
            }
            checkUpgrade -> {
                Updater.checkUpdate(callback = {
                    if (it.getBoolean(Updater.UPGRADABLE)) {
                        UpgradeDialog.create(it).show(supportFragmentManager, "UPGRADE_DIALOG")
                        if (Setting.instance.ignoreUpgradeVersionCode >= it.getInt(Updater.VERSIONCODE)) {
                            toast(getString(R.string.upgrade_ignored))
                        }
                    } else {
                        toast(getText(R.string.no_newer_version))
                    }
                }, force = true)
            }
            licenses -> {
                showLicenseDialog()
            }
            intro -> {
                startActivity(Intent(this, AppIntroActivity::class.java))
            }
            followOnTwitter -> {
                openUrl(TWITTER)
            }
            forkOnGitHub -> {
                openUrl(GITHUB)
            }
            visitWebsite -> {
                openUrl(WEBSITE)
            }
            reportBugs -> {
                startActivity(Intent(this, BugReportActivity::class.java))
            }
            writeAnEmail -> {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:contact@kabouzeid.com")
                intent.putExtra(Intent.EXTRA_EMAIL, "contact@kabouzeid.com")
                intent.putExtra(Intent.EXTRA_SUBJECT, "Phonograph")
                startActivity(Intent.createChooser(intent, "E-Mail"))
            }
            translate -> {
                openUrl(TRANSLATE)
            }
            aidanFollestadGitHub -> {
                openUrl(AIDAN_FOLLESTAD_GITHUB)
            }
            michaelCookWebsite -> {
                openUrl(MICHAEL_COOK_WEBSITE)
            }
            maartenCorpelWebsite -> {
                openUrl(MAARTEN_CORPEL_WEBSITE)
            }
            maartenCorpelTwitter -> {
                openUrl(MAARTEN_CORPEL_TWITTER)
            }
            aleksandarTesicTwitter -> {
                openUrl(ALEKSANDAR_TESIC_TWITTER)
            }
            eugeneCheungGitHub -> {
                openUrl(EUGENE_CHEUNG_GITHUB)
            }
            eugeneCheungWebsite -> {
                openUrl(EUGENE_CHEUNG_WEBSITE)
            }
            adrianTwitter -> {
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
        val app = instance
        LicensesDialog.Builder(this)
            .setNotices(R.raw.notices)
            .setTitle(R.string.licenses)
            .setNoticesCssStyle(
                getString(R.string.license_dialog_style)
                    .replace("{bg-color}", if (app.nightmode()) "424242" else "ffffff")
                    .replace("{text-color}", if (app.nightmode()) "ffffff" else "000000")
                    .replace("{license-bg-color}", if (app.nightmode()) "535353" else "eeeeee")
            )
            .setIncludeOwnLicense(true)
            .build()
            .show()
    }

    companion object {
        private const val GITHUB = "https://github.com/chr56/Phonograph_Plus"
        private const val TWITTER = "https://twitter.com/swiftkarim"
        private const val WEBSITE = "https://kabouzeid.com/"

        private const val GITHUB_MODIFIER = "https://github.com/chr56/"

        private const val TRANSLATE =
            "https://crowdin.com/project/phonograph-plus"
//            "https://phonograph.oneskyapp.com/collaboration/project?id=26521"
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
