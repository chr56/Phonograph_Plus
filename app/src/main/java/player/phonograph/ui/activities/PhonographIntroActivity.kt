/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.SlideBackgroundColorHolder
import com.github.appintro.SlidePolicy
import com.google.android.material.snackbar.Snackbar
import lib.activityresultcontract.IRequestPermission
import lib.activityresultcontract.RequestPermissionDelegate
import lib.activityresultcontract.registerActivityResultLauncherDelegate
import lib.storage.launcher.IOpenFileStorageAccessible
import lib.storage.launcher.OpenDocumentContract
import lib.storage.launcher.OpenFileStorageAccessDelegate
import player.phonograph.R
import player.phonograph.databinding.FragmentIntroBinding
import player.phonograph.databinding.FragmentIntroSlideSettingBinding
import player.phonograph.databinding.ItemSimpleBinding
import player.phonograph.mechanism.backup.Backup
import player.phonograph.settings.Keys
import player.phonograph.settings.PrerequisiteSetting
import player.phonograph.settings.Setting
import player.phonograph.ui.dialogs.BackupImportDialog
import player.phonograph.util.permissions.PermissionDetail
import player.phonograph.util.permissions.hasPermission
import player.phonograph.util.permissions.necessaryPermissions
import player.phonograph.util.permissions.permissionDescription
import player.phonograph.util.permissions.permissionName
import androidx.annotation.StringRes
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import util.theme.materials.R as MR

class PhonographIntroActivity : AppIntro(), IOpenFileStorageAccessible, IRequestPermission {

    private fun config() {
        isWizardMode = true
        showStatusBar(true)
        setStatusBarColorRes(util.theme.materials.R.color.md_black_1000)
        setNavBarColorRes(util.theme.materials.R.color.md_black_1000)
        isColorTransitionsEnabled = true
    }

    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val requestPermissionDelegate: RequestPermissionDelegate = RequestPermissionDelegate()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config()
        registerActivityResultLauncherDelegate(openFileStorageAccessDelegate, requestPermissionDelegate)

        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.app_name),
                description = getString(R.string.welcome_to_phonograph),
                imageDrawable = R.drawable.icon_web,
                backgroundColorRes = MR.color.md_blue_900
            )
        )

        addSlide(
            PermissionSlideFragment.newInstance()
        )

        addSlide(
            SettingSlideFragment.newInstance()
        )

        addSlide(
            AppIntroFragment.createInstance(
                description = getString(R.string.completed),
                imageDrawable = R.drawable.icon_web,
                backgroundColorRes = MR.color.md_green_800
            )
        )

    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        PrerequisiteSetting.instance(this).introShown = false
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        startActivity(MainActivity.launchingIntent(this))
        PrerequisiteSetting.instance(this).introShown = true
        finish()
    }

    class PermissionSlideFragment : EmptySlideFragment(), SlidePolicy {

        override val titleRes: Int get() = R.string.grant_permission
        override val descriptionRes: Int get() = R.string.grant_permission_description


        private var _items: List<ItemSimpleBinding>? = null
        private val items get() = _items!!

        override fun setUpView(container: ViewGroup) {
            _items = necessaryPermissions.map { detail ->
                createViewItem(detail) { view ->
                    val context = container.context
                    val permissionsTool = (context as? IRequestPermission)?.requestPermissionDelegate
                    if (permissionsTool != null) {
                        permissionsTool.launch(detail.permission) {
                            updateItemBackgroundColor(view, detail.permission)
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "${context::class.java.simpleName} is unsupported for ask permissions",
                            LENGTH_LONG
                        ).show()
                    }
                }
            }
            val params = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(32)
            }
            items.forEachIndexed { index, itemBinding ->
                container.addView(itemBinding.root, index, params)
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _items = null
        }

        private fun createViewItem(
            detail: PermissionDetail,
            onClick: OnClickListener,
        ): ItemSimpleBinding {
            return ItemSimpleBinding.inflate(layoutInflater).also { itemBinding ->
                val context = itemBinding.root.context
                itemBinding.title.text = permissionName(context, detail.permission)
                itemBinding.text.text = permissionDescription(context, detail.permission)
                itemBinding.root.setOnClickListener(onClick)
                itemBinding.menu.visibility = GONE
                updateItemBackgroundColor(itemBinding.root, detail.permission)
            }
        }

        private fun updateItemBackgroundColor(view: View, permission: String) {
            val granted = hasPermission(requireContext(), permission)
            view.setBackgroundColor(
                resources.getColor(
                    if (granted) MR.color.md_green_600 else MR.color.md_red_600, null
                )
            )
        }

        override val isPolicyRespected: Boolean
            get() {
                val context = requireContext()
                for (permission in necessaryPermissions) {
                    val result = context.checkSelfPermission(permission.permission)
                    if (result == PackageManager.PERMISSION_DENIED && permission.required) {
                        Snackbar.make(binding.container, R.string.permissions_denied, Snackbar.LENGTH_SHORT).show()
                        return false
                    }
                }
                return true
            }

        override fun onUserIllegallyRequestedNextPage() {
        }

        override val defaultBackgroundColorRes: Int get() = MR.color.md_yellow_900

        companion object {
            fun newInstance(): PermissionSlideFragment = PermissionSlideFragment()
        }
    }

    class SettingSlideFragment : EmptySlideFragment(), SlidePolicy {

        override val titleRes: Int get() = R.string.action_settings
        override val descriptionRes: Int get() = -1

        private var _contentBinding: FragmentIntroSlideSettingBinding? = null
        val contentBinding: FragmentIntroSlideSettingBinding get() = _contentBinding!!

        override fun setUpView(container: ViewGroup) {
            _contentBinding = FragmentIntroSlideSettingBinding.inflate(layoutInflater)
            container.removeAllViews()
            container.addView(contentBinding.root)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val msg =
                "${getString(R.string.pref_summary_auto_check_for_updates)}\n\n${getString(R.string.pref_description_auto_check_for_updates_extra_description)}"
            contentBinding.checkUpgradeDesc.text = msg
            contentBinding.checkUpgradeChoose.setOnCheckedChangeListener { _, selected ->
                when (selected) {
                    R.id.enable  -> Setting(view.context)[Keys.checkUpgradeAtStartup].data = true
                    R.id.disable -> Setting(view.context)[Keys.checkUpgradeAtStartup].data = false
                }
            }
            contentBinding.backup.text = getString(R.string.action_import, getString(R.string.action_backup))
            contentBinding.backup.setOnClickListener {
                val activity = activity
                require(activity is IOpenFileStorageAccessible)
                activity.openFileStorageAccessDelegate.launch(
                    OpenDocumentContract.Config(arrayOf("*/*"))
                ) { uri ->
                    uri ?: return@launch
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        activity.contentResolver.openFileDescriptor(uri, "r")?.use {
                            FileInputStream(it.fileDescriptor).use { stream ->
                                val sessionId =
                                    Backup.Import.startImportBackupFromArchive(context = activity, stream)
                                launch(Dispatchers.Main) {
                                    BackupImportDialog.newInstance(sessionId)
                                        .show(activity.supportFragmentManager, "IMPORT")
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _contentBinding = null
        }

        override val isPolicyRespected: Boolean
            get() = contentBinding.checkUpgradeChoose.checkedRadioButtonId != -1

        override fun onUserIllegallyRequestedNextPage() {
            Toast.makeText(requireContext(), R.string.choose_at_least_one, Toast.LENGTH_SHORT)
                .show()
        }

        override val defaultBackgroundColorRes: Int get() = MR.color.md_deep_purple_800

        companion object {
            fun newInstance(): SettingSlideFragment = SettingSlideFragment()
        }
    }

    /**
     * basic fragment of a slide, with title and description, no content
     */
    abstract class EmptySlideFragment : Fragment(), SlideBackgroundColorHolder {


        @get:StringRes
        protected abstract val titleRes: Int
        @get:StringRes
        protected abstract val descriptionRes: Int

        protected val title: String? get() = if (titleRes != -1) getString(titleRes) else null
        protected val description: String? get() = if (descriptionRes != -1) getString(descriptionRes) else null

        private var _binding: FragmentIntroBinding? = null
        val binding: FragmentIntroBinding get() = _binding!!

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View {
            _binding = FragmentIntroBinding.inflate(inflater, container, false)
            binding.title.text = title
            binding.description.text = description
            setUpView(binding.container)
            return binding.root
        }

        protected open fun setUpView(container: ViewGroup) {}

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }

        @Deprecated(
            "`defaultBackgroundColor` has been deprecated to support configuration changes",
            replaceWith = ReplaceWith("defaultBackgroundColorRes")
        )
        override val defaultBackgroundColor: Int get() = resources.getColor(defaultBackgroundColorRes, null)
        override fun setBackgroundColor(backgroundColor: Int) {
            binding.root.setBackgroundColor(backgroundColor)
        }
    }
}