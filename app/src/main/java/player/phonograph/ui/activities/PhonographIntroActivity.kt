/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.SlideBackgroundColorHolder
import com.github.appintro.SlidePolicy
import player.phonograph.R
import player.phonograph.databinding.FragmentIntroBinding
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class PhonographIntroActivity : AppIntro() {

    private fun config() {
        showStatusBar(true)
        isColorTransitionsEnabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config()

        addSlide(
            AppIntroFragment.createInstance(
                title = getString(R.string.app_name),
                description = getString(R.string.welcome_to_phonograph),
                imageDrawable = R.drawable.icon_web,
                backgroundColorRes = R.color.md_blue_900
            )
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
            _binding = FragmentIntroBinding.inflate(inflater, container, true)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            binding.title.text = getString(titleRes)
            binding.description.text = getString(descriptionRes)
            setUpView(binding.container)
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