/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import player.phonograph.R
import androidx.fragment.app.Fragment
import android.os.Bundle

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
}