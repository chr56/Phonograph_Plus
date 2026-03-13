/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.setting

import player.phonograph.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import android.content.res.Resources

class SettingsViewModel : ViewModel() {
    typealias Page = String

    private var _currentPage = mutableStateOf<Page>(PAGE_HOME)
    val currentPage by _currentPage

    fun currentTitle(resources: Resources): String = resources.getString(
           when (_currentPage.value) {
               PAGE_APPEARANCE   -> R.string.pref_category_appearance
               PAGE_CONTENT      -> R.string.pref_category_content
               PAGE_BEHAVIOUR    -> R.string.pref_category_behaviour
               PAGE_NOTIFICATION -> R.string.pref_category_notification
               PAGE_ADVANCED     -> R.string.pref_category_advanced
               PAGE_UPDATES      -> R.string.pref_category_updates
               else              -> R.string.action_settings
           }
       )


    fun updatePage(newPage: Page) {
        _currentPage.value = newPage
    }

    companion object {
        const val PAGE_HOME = "home"
        const val PAGE_APPEARANCE = "appearance"
        const val PAGE_CONTENT = "content"
        const val PAGE_BEHAVIOUR = "behaviours"
        const val PAGE_NOTIFICATION = "notification"
        const val PAGE_ADVANCED = "advanced"
        const val PAGE_UPDATES = "updates"
    }
}