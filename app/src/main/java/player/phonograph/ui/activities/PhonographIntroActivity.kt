/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.activities

import com.github.appintro.AppIntro
import androidx.fragment.app.Fragment
import android.os.Bundle

class PhonographIntroActivity : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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