package player.phonograph.ui.activities.bugreport

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringDef
import androidx.annotation.StringRes
import util.mdcolor.pref.ThemeColor
import util.mddesign.util.TintHelper
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton.OnVisibilityChangedListener
import com.google.android.material.textfield.TextInputLayout
import org.eclipse.egit.github.core.Issue
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.client.RequestException
import org.eclipse.egit.github.core.service.IssueService
import player.phonograph.R
import player.phonograph.databinding.ActivityBugReportBinding
import player.phonograph.misc.DialogAsyncTask
import player.phonograph.ui.activities.base.ThemeActivity
import player.phonograph.ui.activities.bugreport.model.DeviceInfo
import player.phonograph.ui.activities.bugreport.model.Report
import player.phonograph.ui.activities.bugreport.model.github.ExtraInfo
import player.phonograph.ui.activities.bugreport.model.github.GithubLogin
import player.phonograph.ui.activities.bugreport.model.github.GithubTarget
import java.io.IOException

class BugReportActivity : ThemeActivity() {

    private lateinit var deviceInfo: DeviceInfo

    private var activityBinding: ActivityBugReportBinding? = null
    private val binding get() = activityBinding!!
//
//    private var viewBinding: BugReportCardReportBinding? = null
//    private val v get() = viewBinding!!
//
//    private var viewBindingInfoCard: BugReportCardDeviceInfoBinding? = null
//    private val infoCard get() = viewBindingInfoCard!!

    @StringDef(RESULT_OK, RESULT_BAD_CREDENTIALS, RESULT_INVALID_TOKEN, RESULT_ISSUES_NOT_ENABLED, RESULT_UNKNOWN)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    private annotation class Result

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        viewBinding = BugReportCardReportBinding.inflate(layoutInflater)
//        viewBindingInfoCard = BugReportCardDeviceInfoBinding.inflate(layoutInflater)
        activityBinding = ActivityBugReportBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        initViews()

        if (TextUtils.isEmpty(title)) setTitle(R.string.report_an_issue)

        deviceInfo = DeviceInfo(this)

        binding.infoCard.airTextDeviceInfo.text = deviceInfo.toString()
    }

    private fun initViews() {
        val accentColor = ThemeColor.accentColor(this)
        val primaryColor = ThemeColor.primaryColor(this)

        binding.toolbar.setBackgroundColor(primaryColor)
        setSupportActionBar(binding.toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        TintHelper.setTintAuto(binding.v.optionUseAccount, accentColor, false)
        binding.v.optionUseAccount.setOnClickListener {
            binding.v.inputTitle.isEnabled = true
            binding.v.inputDescription.isEnabled = true
            binding.v.inputUsername.isEnabled = true
            binding.v.inputPassword.isEnabled = true
            binding.v.optionAnonymous.isChecked = false
            binding.buttonSend.hide(object : OnVisibilityChangedListener() {
                override fun onHidden(fab: FloatingActionButton) {
                    super.onHidden(fab)
                    binding.buttonSend.setImageResource(R.drawable.ic_send_white_24dp)
                    binding.buttonSend.show()
                }
            })
        }

        TintHelper.setTintAuto(binding.v.optionAnonymous, accentColor, false)
        binding.v.optionAnonymous.setOnClickListener {
            binding.v.inputTitle.isEnabled = false
            binding.v.inputDescription.isEnabled = false
            binding.v.inputUsername.isEnabled = false
            binding.v.inputPassword.isEnabled = false
            binding.v.optionUseAccount.isChecked = false
            binding.buttonSend.hide(object : OnVisibilityChangedListener() {
                override fun onHidden(fab: FloatingActionButton) {
                    super.onHidden(fab)
                    binding.buttonSend.setImageResource(R.drawable.ic_open_in_browser_white_24dp)
                    binding.buttonSend.show()
                }
            })
        }

        binding.v.inputPassword.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                reportIssue()
                return@setOnEditorActionListener true
            }
            false
        }

        binding.infoCard.airTextDeviceInfo.setOnClickListener { copyDeviceInfoToClipBoard() }

        TintHelper.setTintAuto(binding.buttonSend, accentColor, true)
        binding.buttonSend.setOnClickListener { reportIssue() }

        TintHelper.setTintAuto(binding.v.inputTitle, accentColor, false)
        TintHelper.setTintAuto(binding.v.inputDescription, accentColor, false)
        TintHelper.setTintAuto(binding.v.inputUsername, accentColor, false)
        TintHelper.setTintAuto(binding.v.inputPassword, accentColor, false)
    }

    private fun reportIssue() {
        if (binding.v.optionUseAccount.isChecked) {
            if (!validateInput()) return
            val username = binding.v.inputUsername.text.toString()
            val password = binding.v.inputPassword.text.toString()
            sendBugReport(GithubLogin(username, password))
        } else {
            copyDeviceInfoToClipBoard()
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    this.data = Uri.parse(ISSUE_TRACKER_LINK)
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }

    private fun copyDeviceInfoToClipBoard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.device_info), deviceInfo!!.toMarkdown())
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            this@BugReportActivity,
            R.string.copied_device_info_to_clipboard,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun validateInput(): Boolean {
        var hasErrors = false
        if (binding.v.optionUseAccount.isChecked) {
            if (TextUtils.isEmpty(binding.v.inputUsername.text)) {
                setError(binding.v.inputLayoutUsername, R.string.bug_report_no_username)
                hasErrors = true
            } else {
                removeError(binding.v.inputLayoutUsername)
            }
            if (TextUtils.isEmpty(binding.v.inputPassword.text)) {
                setError(binding.v.inputLayoutPassword, R.string.bug_report_no_password)
                hasErrors = true
            } else {
                removeError(binding.v.inputLayoutPassword)
            }
        }
        if (TextUtils.isEmpty(binding.v.inputTitle.text)) {
            setError(binding.v.inputLayoutTitle, R.string.bug_report_no_title)
            hasErrors = true
        } else {
            removeError(binding.v.inputLayoutTitle)
        }
        if (TextUtils.isEmpty(binding.v.inputDescription.text)) {
            setError(binding.v.inputLayoutDescription, R.string.bug_report_no_description)
            hasErrors = true
        } else {
            removeError(binding.v.inputLayoutDescription)
        }
        return !hasErrors
    }

    private fun setError(editTextLayout: TextInputLayout?, @StringRes errorRes: Int) {
        editTextLayout!!.error = getString(errorRes)
    }

    private fun removeError(editTextLayout: TextInputLayout?) {
        editTextLayout!!.error = null
    }

    private fun sendBugReport(login: GithubLogin) {
        if (!validateInput()) return
        val bugTitle = binding.v.inputTitle.text.toString()
        val bugDescription = binding.v.inputDescription.text.toString()
        val report = Report(bugTitle, bugDescription, deviceInfo, ExtraInfo())
        val target = GithubTarget("chr56", "Phonograph_Plus")
        ReportIssueAsyncTask.report(this, report, target, login)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private class ReportIssueAsyncTask private constructor(
        activity: Activity,
        private val report: Report,
        private val target: GithubTarget,
        private val login: GithubLogin
    ) : DialogAsyncTask<Void?, Void?, String?>(activity) {
        override fun createDialog(context: Context): Dialog {
            return MaterialDialog(context).title(R.string.bug_report_uploading, null)
        }

        @Result
        override fun doInBackground(vararg params: Void?): String? {
            val client: GitHubClient = if (login.shouldUseApiToken()) {
                GitHubClient().setOAuth2Token(login.apiToken)
            } else {
                GitHubClient().setCredentials(login.username, login.password)
            }
            val issue = Issue().setTitle(report.title).setBody(report.description)
            return try {
                IssueService(client).createIssue(target.username, target.repository, issue)
                RESULT_OK
            } catch (e: RequestException) {
                when (e.status) {
                    STATUS_BAD_CREDENTIALS -> {
                        if (login.shouldUseApiToken()) RESULT_INVALID_TOKEN else RESULT_BAD_CREDENTIALS
                    }
                    STATUS_ISSUES_NOT_ENABLED -> RESULT_ISSUES_NOT_ENABLED
                    else -> {
                        e.printStackTrace()
                        RESULT_UNKNOWN
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                RESULT_UNKNOWN
            }
        }

        override fun onPostExecute(@Result result: String?) {
            super.onPostExecute(result)
            val context = context ?: return
            when (result) {
                RESULT_OK -> tryToFinishActivity()
                RESULT_BAD_CREDENTIALS -> MaterialDialog(context)
                    .title(R.string.bug_report_failed, null)
                    .message(R.string.bug_report_failed_wrong_credentials, null, null)
                    .positiveButton(android.R.string.ok, null, null)
                    .show()
                RESULT_INVALID_TOKEN -> MaterialDialog(context)
                    .title(R.string.bug_report_failed, null)
                    .message(R.string.bug_report_failed_invalid_token, null, null)
                    .positiveButton(android.R.string.ok, null, null)
                    .show()
                RESULT_ISSUES_NOT_ENABLED -> MaterialDialog(context)
                    .title(R.string.bug_report_failed, null)
                    .message(R.string.bug_report_failed_issues_not_available, null, null)
                    .positiveButton(android.R.string.ok, null, null)
                    .show()
                else -> MaterialDialog(context)
                    .title(R.string.bug_report_failed, null)
                    .message(R.string.bug_report_failed_unknown, null, null)
                    .positiveButton(android.R.string.ok, null) {
                        tryToFinishActivity()
                    }
                    .negativeButton(android.R.string.cancel, null) {
                        tryToFinishActivity()
                    }
                    .show()
            }
        }

        private fun tryToFinishActivity() {
            val context = context
            if (context is Activity && !context.isFinishing) {
                context.finish()
            }
        }

        companion object {
            fun report(
                activity: Activity,
                report: Report,
                target: GithubTarget,
                login: GithubLogin
            ) {
                ReportIssueAsyncTask(activity, report, target, login).execute()
            }
        }
    }

    companion object {
        private const val STATUS_BAD_CREDENTIALS = 401
        private const val STATUS_ISSUES_NOT_ENABLED = 410
        private const val RESULT_OK = "RESULT_OK"
        private const val RESULT_BAD_CREDENTIALS = "RESULT_BAD_CREDENTIALS"
        private const val RESULT_INVALID_TOKEN = "RESULT_INVALID_TOKEN"
        private const val RESULT_ISSUES_NOT_ENABLED = "RESULT_ISSUES_NOT_ENABLED"
        private const val RESULT_UNKNOWN = "RESULT_UNKNOWN"
        private const val ISSUE_TRACKER_LINK = "https://github.com/chr56/Phonograph_Plus/issues"
    }
}
