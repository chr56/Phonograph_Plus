/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.SlideBackgroundColorHolder
import com.github.appintro.SlidePolicy
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.IRequestPermission
import lib.phonograph.misc.OpenDocumentContract
import lib.phonograph.misc.OpenFileStorageAccessTool
import lib.phonograph.misc.RequestPermissionTool
import player.phonograph.R
import player.phonograph.databinding.FragmentIntroBinding
import player.phonograph.databinding.FragmentIntroSlideSettingBinding
import player.phonograph.databinding.ItemSimpleBinding
import player.phonograph.mechanism.backup.Backup
import player.phonograph.settings.Setting
import player.phonograph.ui.dialogs.BackupImportDialog
import player.phonograph.util.permissions.GrantedPermission
import player.phonograph.util.permissions.checkPermission
import androidx.annotation.StringRes
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.Manifest
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
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

class PhonographIntroActivity : AppIntro(), IOpenFileStorageAccess, IRequestPermission {

    private fun config() {
        isWizardMode = true
        showStatusBar(true)
        setStatusBarColorRes(R.color.md_black_1000)
        setNavBarColorRes(R.color.md_black_1000)
        isColorTransitionsEnabled = true
    }

    override val openFileStorageAccessTool: OpenFileStorageAccessTool = OpenFileStorageAccessTool()
    override val requestPermissionTool: RequestPermissionTool = RequestPermissionTool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config()
        openFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        requestPermissionTool.register(lifecycle, activityResultRegistry)

        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.app_name),
                description = getString(R.string.welcome_to_phonograph),
                imageDrawable = R.drawable.icon_web,
                backgroundColorRes = R.color.md_blue_900
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
                backgroundColorRes = R.color.md_green_800
            )
        )

    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        Setting.instance.introShown = false
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        startActivity(Intent(this, MainActivity::class.java))
        Setting.instance.introShown = true
        finish()
    }

    class PermissionSlideFragment : EmptySlideFragment(), SlidePolicy {

        override val titleRes: Int get() = R.string.grant_permission
        override val descriptionRes: Int get() = R.string.grant_permission_description


        class PermissionDetail(
            val permission: String,
            val title: String,
            val description: String,
        )

        val permissions: List<PermissionDetail>
            get() = when {
                SDK_INT >= TIRAMISU ->
                    listOf(
                        PermissionDetail(
                            Manifest.permission.POST_NOTIFICATIONS,
                            getString(R.string.permission_name_post_notifications),
                            getString(R.string.permission_desc_post_notifications)
                        ),
                        PermissionDetail(
                            Manifest.permission.READ_MEDIA_AUDIO,
                            getString(R.string.permission_name_read_media_audio),
                            getString(R.string.permission_desc_read_media_audio)
                        ),
                    )
                else                ->
                    listOf(
                        PermissionDetail(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            getString(R.string.permission_name_read_external_storage),
                            getString(R.string.permission_desc_read_external_storage),
                        )
                    )
            }

        private var _items: List<ItemSimpleBinding>? = null
        private val items get() = _items!!
        override fun setUpView(container: ViewGroup) {
            _items = permissions.map { detail ->
                createViewItem(detail) { view ->
                    val context = container.context
                    val permissionsTool = (context as? IRequestPermission)?.requestPermissionTool
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
                itemBinding.title.text = detail.title
                itemBinding.text.text = detail.description
                itemBinding.root.setOnClickListener(onClick)
                itemBinding.menu.visibility = GONE
                updateItemBackgroundColor(itemBinding.root, detail.permission)
            }
        }

        private fun updateItemBackgroundColor(view: View, permission: String) {
            val permissionResult = checkPermission(requireContext(), permission)
            view.setBackgroundColor(
                resources.getColor(
                    if (permissionResult is GrantedPermission) R.color.md_green_600
                    else R.color.md_red_600, null
                )
            )
        }

        override val isPolicyRespected: Boolean
            get() = true

        override fun onUserIllegallyRequestedNextPage() {
        }

        override val defaultBackgroundColorRes: Int get() = R.color.md_yellow_900

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
                "${getString(R.string.auto_check_upgrade_summary)}\n\n${getString(R.string.auto_check_upgrade_extra_description)}"
            contentBinding.checkUpgradeDesc.text = msg
            contentBinding.checkUpgradeChoose.setOnCheckedChangeListener { _, selected ->
                when (selected) {
                    R.id.enable  -> Setting.instance.checkUpgradeAtStartup = true
                    R.id.disable -> Setting.instance.checkUpgradeAtStartup = false
                }
            }
            contentBinding.backup.text = getString(R.string.action_import, getString(R.string.action_backup))
            contentBinding.backup.setOnClickListener {
                val activity = activity
                require(activity is IOpenFileStorageAccess)
                activity.openFileStorageAccessTool.launch(
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

        override val defaultBackgroundColorRes: Int get() = R.color.md_deep_purple_800

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