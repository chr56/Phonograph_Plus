/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.setting

import player.phonograph.R
import player.phonograph.settings.PreferenceKey
import player.phonograph.settings.Setting
import player.phonograph.util.concurrent.lifecycleScopeOrNewOne
import player.phonograph.util.theme.tintButtons
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.addCallback
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.launch


private typealias Page = String

private const val PAGE_HOME = "home"
private const val PAGE_APPEARANCE = "appearance"
private const val PAGE_CONTENT = "content"
private const val PAGE_BEHAVIOUR = "behaviours"
private const val PAGE_NOTIFICATION = "notification"
private const val PAGE_ADVANCED = "advanced"
private const val PAGE_UPDATES = "updates"

@Composable
fun PhonographPreferenceScreen(
    onBackPressedDispatcher: OnBackPressedDispatcher,
    onUpdateTitle: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    var currentPage by rememberSaveable(key = "page") { mutableStateOf<String>(PAGE_HOME) }
    LaunchedEffect(currentPage) {
        onUpdateTitle(localize(context.resources, currentPage))
        if (currentPage != PAGE_HOME) {
            onBackPressedDispatcher.addCallback(lifecycleOwner, true) {
                remove()
                currentPage = PAGE_HOME
            }
        }
    }
    Surface(Modifier.fillMaxSize()) {
        AnimatedContent(
            currentPage,
            transitionSpec = { animation(targetState == PAGE_HOME) }
        ) { target ->
            when (target) {
                PAGE_HOME         -> PreferenceScreenHome { currentPage = it }
                PAGE_APPEARANCE   -> PreferenceScreenAppearance()
                PAGE_CONTENT      -> PreferenceScreenContent()
                PAGE_BEHAVIOUR    -> PreferenceScreenBehaviour()
                PAGE_NOTIFICATION -> PreferenceScreenNotification()
                PAGE_ADVANCED     -> PreferenceScreenAdvanced()
                PAGE_UPDATES      -> PreferenceScreenUpdates()
                else              -> Text(text = stringResource(R.string.msg_empty))
            }
        }

    }
}

@Composable
private fun PreferenceScreenHome(navigateTo: (Page) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        ProvideTextStyle(MaterialTheme.typography.subtitle1) {
            SettingCategory(R.string.pref_category_appearance, R.drawable.ic_palette_white_24dp) {
                navigateTo(PAGE_APPEARANCE)
            }
            SettingCategory(R.string.pref_category_content, R.drawable.ic_library_music_white_24dp) {
                navigateTo(PAGE_CONTENT)
            }
            SettingCategory(R.string.pref_category_behaviour, R.drawable.ic_play_arrow_white_24dp) {
                navigateTo(PAGE_BEHAVIOUR)
            }
            SettingCategory(R.string.pref_category_notification, R.drawable.ic_notifications_white_24dp) {
                navigateTo(PAGE_NOTIFICATION)
            }
            SettingCategory(R.string.pref_category_advanced, R.drawable.ic_developer_mode_white_24dp) {
                navigateTo(PAGE_ADVANCED)
            }
            SettingCategory(R.string.pref_category_updates, R.drawable.ic_upgrade_white_24dp) {
                navigateTo(PAGE_UPDATES)
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}

@Composable
private fun SettingCategory(
    @StringRes category: Int,
    @DrawableRes icon: Int,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val text = stringResource(category)
        Box(Modifier.padding(start = 16.dp)) {
            Icon(
                painter = painterResource(icon),
                contentDescription = text,
                modifier = Modifier.size(28.dp),
                tint = colorResource(R.color.icon_lightdark)
            )
        }
        Text(
            text,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        )
    }
}


private fun animation(reverse: Boolean): ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally { if (reverse) -it else it },
    initialContentExit = slideOutHorizontally { if (reverse) it else -it },
)


private fun localize(resources: Resources, page: Page): String = resources.getString(
    when (page) {
        PAGE_APPEARANCE   -> R.string.pref_category_appearance
        PAGE_CONTENT      -> R.string.pref_category_content
        PAGE_BEHAVIOUR    -> R.string.pref_category_behaviour
        PAGE_NOTIFICATION -> R.string.pref_category_notification
        PAGE_ADVANCED     -> R.string.pref_category_advanced
        PAGE_UPDATES      -> R.string.pref_category_updates
        else              -> R.string.action_settings
    }
)

@Composable
fun <T> dependOn(key: PreferenceKey<T>, predicate: (T) -> Boolean): Boolean {
    return if (LocalInspectionMode.current) {
        false
    } else {
        val context = LocalContext.current
        val preference = remember { Setting(context)[key] }
        val state by preference.flow.collectAsState(preference.default)
        predicate(state)
    }
}


fun resetPreference(context: Context, @StringRes what: Int, vararg keys: PreferenceKey<*>) {
    AlertDialog.Builder(context)
        .setTitle(context.getString(R.string.action_reset))
        .setMessage(context.getString(what))
        .setPositiveButton(android.R.string.ok) { _, _ ->
            val targets = keys.map { Setting(context)[it] }
            context.lifecycleScopeOrNewOne().launch {
                for (preference in targets) {
                    preference.reset()
                }
            }
        }
        .setNegativeButton(android.R.string.cancel) { _, _ -> }
        .show().tintButtons()
}