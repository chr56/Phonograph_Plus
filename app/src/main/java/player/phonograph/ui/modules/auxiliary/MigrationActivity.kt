package player.phonograph.ui.modules.auxiliary

import player.phonograph.R
import player.phonograph.mechanism.migrate.MigrationManager
import player.phonograph.ui.basis.ComposeActivity
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.SystemBarsPadded
import player.phonograph.ui.modules.main.MainActivity
import player.phonograph.util.permissions.navigateToAppDetailSetting
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import android.content.res.Resources
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MigrationActivity : ComposeActivity() {

    private val migrationResultFlow = MutableStateFlow<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val result by migrationResultFlow.collectAsState()
            val color = remember { derivedStateOf { if (result != null) colorDone else colorProcess } }
            PhonographTheme(color) {
                SystemBarsPadded {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(stringResource(R.string.version_migration)) },
                                actions = { Options() }
                            )
                        }
                    ) {
                        Column(Modifier.padding(it)) {
                            if (result != null) {
                                ResultScreen(result)
                            } else {
                                OngoingScreen()
                            }
                        }
                        SideEffect {
                            executeMigration()
                        }
                    }
                }
            }
        }
    }

    private enum class ResultScreenContent(
        val icon: ImageVector,
        val continuable: Boolean = true,
        val autoJumpCountdown: Long = 1000,
    ) {
        Success(
            Icons.Default.Done, true, 0
        ) {
            override fun text(resources: Resources): String =
                resources.getString(R.string.success)
        },
        Skipped(
            Icons.Default.Done, true, 500
        ) {
            override fun text(resources: Resources): String =
                resources.getString(R.string.version_migration_message_skipped)
        },
        Warning(
            Icons.Default.Warning, true, -1
        ) {
            override fun text(resources: Resources): String =
                "${resources.getString(R.string.version_migration_hint_too_old)}\n${resources.getString(R.string.version_migration_hint_suggest_to_wipe_data)}"
        },
        Forbidden(
            Icons.Default.Warning, false, -1
        ) {
            override fun text(resources: Resources): String =
                "${resources.getString(R.string.version_migration_hint_too_old)}\n${resources.getString(R.string.version_migration_hint_required_to_wipe_data)}"
        },
        Error(
            Icons.Default.Close, false, -1
        ) {
            override fun text(resources: Resources): String =
                resources.getString(R.string.failed)
        },
        ;

        abstract fun text(resources: Resources): String

    }

    @Composable
    private fun ColumnScope.ResultScreen(resultCode: Int?) {

        val content = when (resultCode) {
            MigrationManager.CODE_SUCCESSFUL -> ResultScreenContent.Success
            MigrationManager.CODE_NO_ACTION -> ResultScreenContent.Skipped
            MigrationManager.CODE_WARNING -> ResultScreenContent.Warning
            MigrationManager.CODE_FORBIDDEN -> ResultScreenContent.Forbidden
            MigrationManager.CODE_UNKNOWN_ERROR -> ResultScreenContent.Error
            else -> ResultScreenContent.Skipped
        }

        val resources = LocalResources.current
        val text = content.text(resources)
        val icon = content.icon
        Image(
            icon, text,
            Modifier
                .padding(48.dp)
                .align(Alignment.CenterHorizontally)
        )
        Text(
            text,
            Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center
        )
        if (content.continuable) TextButton(
            ::gotoMainActivity,
            Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                stringResource(android.R.string.ok),
                Modifier.padding(4.dp)
            )
        }
        LaunchedEffect(content) {
            if (content.autoJumpCountdown > -1) {
                delay(content.autoJumpCountdown)
                gotoMainActivity()
            }
        }
    }

    @Composable
    private fun ColumnScope.OngoingScreen() {
        CircularProgressIndicator(
            Modifier
                .padding(56.dp)
                .align(Alignment.CenterHorizontally)
        )
        Text(
            stringResource(R.string.version_migration_summary),
            Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center
        )
    }

    @Composable
    private fun Options() {
        IconButton(
            content = {
                Icon(Icons.Default.Info, stringResource(id = R.string.label_app_info))
            },
            onClick = {
                navigateToAppDetailSetting(this)
            }
        )
    }


    private fun executeMigration() {
        lifecycleScope.launch {
            delay(500)
            withContext(Dispatchers.IO) {
                migrationResultFlow.value = MigrationManager.migrate(this@MigrationActivity)
            }
        }
    }

    private fun gotoMainActivity() {
        startActivity(MainActivity.launchingIntent(this))
        finish()
    }

    companion object {
        private val colorProcess = Color(red = 33, green = 121, blue = 158)
        private val colorDone = Color(red = 5, green = 141, blue = 124)
    }
}

