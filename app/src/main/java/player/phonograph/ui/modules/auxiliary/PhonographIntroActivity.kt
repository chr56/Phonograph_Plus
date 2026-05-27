/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.auxiliary

import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.SlideBackgroundColorHolder
import com.github.appintro.SlidePolicy
import com.google.android.material.snackbar.Snackbar
import lib.activityresultcontract.IRequestMultiplePermission
import lib.activityresultcontract.IRequestPermission
import lib.activityresultcontract.RequestMultiplePermissionsDelegate
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
import player.phonograph.model.ui.GeneralTheme.Companion.THEME_DARK
import player.phonograph.settings.Keys
import player.phonograph.settings.PrerequisiteSettings
import player.phonograph.settings.Settings
import player.phonograph.ui.dialogs.BackupImportDialog
import player.phonograph.ui.modules.main.MainActivity
import player.phonograph.util.permissions.PermissionDetail
import player.phonograph.util.permissions.hasPermission
import player.phonograph.util.permissions.necessaryPermissions
import player.phonograph.util.permissions.permissionDescription
import player.phonograph.util.permissions.permissionName
import util.theme.materials.MaterialColor
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream

class PhonographIntroActivity : AppIntro(),
                                IOpenFileStorageAccessible,
                                IRequestPermission,
                                IRequestMultiplePermission {

    private fun config() {
        isWizardMode = true
        showStatusBar(true)
        if (SDK_INT >= VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.findViewById<View>(android.R.id.content)?.updateLayoutParams<MarginLayoutParams> {
                    topMargin = insets.top
                    bottomMargin = insets.bottom
                    leftMargin = insets.left
                    rightMargin = insets.right
                }
                windowInsets
            }
        }
        setStatusBarColorRes(MaterialColor.Black._1000.asResource)
        setNavBarColorRes(MaterialColor.Black._1000.asResource)
        isColorTransitionsEnabled = true
    }

    override val openFileStorageAccessDelegate: OpenFileStorageAccessDelegate = OpenFileStorageAccessDelegate()
    override val requestPermissionDelegate: RequestPermissionDelegate = RequestPermissionDelegate()
    override val requestMultiplePermissionsDelegate: RequestMultiplePermissionsDelegate = RequestMultiplePermissionsDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config()
        registerActivityResultLauncherDelegate(
            openFileStorageAccessDelegate, requestPermissionDelegate, requestMultiplePermissionsDelegate
        )

        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.app_name),
                description = getString(R.string.tips_welcome_to_phonograph),
                imageDrawable = R.drawable.icon_web,
                backgroundColorRes = MaterialColor.Blue._900.asResource
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
                description = getString(R.string.state_completed),
                imageDrawable = R.drawable.icon_web,
                backgroundColorRes = MaterialColor.Green._800.asResource
            )
        )

    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        PrerequisiteSettings.instance(this).introShown = false
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        startActivity(MainActivity.launchingIntent(this))
        PrerequisiteSettings.instance(this).introShown = true
        finish()
    }

    class PermissionSlideFragment : EmptySlideFragment(), SlidePolicy {

        override val titleRes: Int get() = R.string.action_grant_permission
        override val descriptionRes: Int get() = R.string.tips_grant_permission_description


        private var _items: List<ItemSimpleBinding>? = null
        private val items get() = _items!!

        override fun setUpView(container: ViewGroup) {
            _items = necessaryPermissions.map { detail -> createPermissionViewBinding(detail) }
            val params = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { setMargins(32) }
            for (itemBinding in items) {
                container.addView(itemBinding.root, params)
            }
            container.addView(
                createGrantAllView(container.context),
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    setMargins(32)
                    gravity = Gravity.START
                }
            )
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _items = null
        }

        private fun createPermissionViewBinding(detail: PermissionDetail): ItemSimpleBinding =
            ItemSimpleBinding.inflate(layoutInflater).also { itemBinding ->
                val context = itemBinding.root.context
                itemBinding.title.text = permissionName(context, detail.permission)
                itemBinding.text.text = permissionDescription(context, detail.permission)
                itemBinding.menu.visibility = GONE
                updateItemBackgroundColor(itemBinding.root, detail.permission)

                itemBinding.root.setOnClickListener { view ->
                    val delegate = (view.context as? IRequestPermission)?.requestPermissionDelegate
                    if (delegate != null) {
                        delegate.launch(detail.permission) {
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

        private fun createGrantAllView(context: Context): View =
            AppCompatButton(context).apply {
                text = context.getString(R.string.action_grant_all)
                setOnClickListener { view ->
                    val delegate = (view.context as? IRequestMultiplePermission)?.requestMultiplePermissionsDelegate
                    if (delegate != null) {
                        val permissions = necessaryPermissions.map { it.permission }.toTypedArray()
                        delegate.launch(permissions) {
                            try {
                                for ((index, binding) in items.withIndex()) {
                                    updateItemBackgroundColor(binding.root, permissions[index])
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
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

        private fun updateItemBackgroundColor(view: View, permission: String) {
            val granted = hasPermission(requireContext(), permission)
            view.setBackgroundColor(
                if (granted) MaterialColor.Green._600.asColor else MaterialColor.Red._600.asColor
            )
        }

        override val isPolicyRespected: Boolean
            get() {
                val context = requireContext()
                for (permission in necessaryPermissions) {
                    val result = context.checkSelfPermission(permission.permission)
                    if (result == PackageManager.PERMISSION_DENIED && permission.required) {
                        Snackbar.make(binding.container, R.string.err_permissions_denied, Snackbar.LENGTH_SHORT).show()
                        return false
                    }
                }
                return true
            }

        override fun onUserIllegallyRequestedNextPage() {
        }

        override val defaultBackgroundColorRes: Int get() = MaterialColor.Yellow._900.asResource

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
                "${getString(R.string.pref_summary_auto_check_for_updates)}\n\n${getString(R.string.pref_summary_auto_check_for_updates_extra_description)}"
            contentBinding.checkUpgradeDesc.text = msg
            contentBinding.checkUpgradeChoose.setOnCheckedChangeListener { _, selected ->
                when (selected) {
                    R.id.enable -> Settings(view.context)[Keys.checkUpgradeAtStartup].data = true
                    R.id.disable -> Settings(view.context)[Keys.checkUpgradeAtStartup].data = false
                }
            }
            contentBinding.backup.text = getString(R.string.action_import, getString(R.string.label_backup))
            contentBinding.backup.setOnClickListener { view ->
                val activity = view.context as FragmentActivity
                (activity as IOpenFileStorageAccessible).openFileStorageAccessDelegate.launch(
                    OpenDocumentContract.Config(arrayOf("*/*"))
                ) { uri ->
                    uri ?: return@launch
                    activity.lifecycleScope.launch(Dispatchers.IO) {
                        activity.contentResolver.openFileDescriptor(uri, "r")?.use {
                            FileInputStream(it.fileDescriptor).use { stream ->
                                val sessionId =
                                    Backup.Import.startImportBackupFromArchive(context = activity, stream)
                                launch(Dispatchers.Main) {
                                    BackupImportDialog.newInstance(sessionId, THEME_DARK)
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
            Toast.makeText(requireContext(), R.string.tips_choose_at_least_one, Toast.LENGTH_SHORT)
                .show()
        }

        override val defaultBackgroundColorRes: Int get() = MaterialColor.DeepPurple._800.asResource

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