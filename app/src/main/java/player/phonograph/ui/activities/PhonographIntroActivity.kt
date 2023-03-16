/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.SlideBackgroundColorHolder
import com.github.appintro.SlidePolicy
import lib.phonograph.misc.IRequestPermission
import lib.phonograph.misc.RequestPermissionTool
import player.phonograph.R
import player.phonograph.databinding.FragmentIntroBinding
import player.phonograph.databinding.ItemSimpleBinding
import player.phonograph.util.Util.warning
import player.phonograph.util.permissions.GrantedPermission
import player.phonograph.util.permissions.checkPermission
import androidx.annotation.StringRes
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import android.Manifest
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

class PhonographIntroActivity : AppIntro(), IRequestPermission {

    private fun config() {
        showStatusBar(true)
        isColorTransitionsEnabled = true
    }

    override val requestPermissionTool: RequestPermissionTool = RequestPermissionTool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config()
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

    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
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

        override val defaultBackgroundColorRes: Int get() = R.color.md_blue_grey_700

        companion object {
            fun newInstance(): PermissionSlideFragment = PermissionSlideFragment()
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

        private var _binding: FragmentIntroBinding? = null
        val binding: FragmentIntroBinding get() = _binding!!

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View {
            _binding = FragmentIntroBinding.inflate(inflater, container, false)
            binding.title.text = getString(titleRes)
            binding.description.text = getString(descriptionRes)
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