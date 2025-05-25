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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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

    private val isCompletedFlow = MutableStateFlow(false)
    private val migrationResultFlow = MutableStateFlow<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isCompleted by isCompletedFlow.collectAsState()
            val color = remember { derivedStateOf { if (isCompleted) colorDone else colorProcess } }
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
                            if (isCompleted) {
                                ResultScreen()
                            } else {
                                OngoingScreen()
                            }
                        }
                        SideEffect {
                            migrateImpl()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ColumnScope.ResultScreen() {
        val code = migrationResultFlow.collectAsState()
        if (code.value == MigrationManager.CODE_SUCCESSFUL) SuccessScreen() else FailedScreen(code.value)
    }

    @Composable
    private fun ColumnScope.SuccessScreen() {
        ResultScreenTemplate(stringResource(R.string.success), Icons.Default.Done, hasButton = true, autoJump = true)
    }

    @Composable
    private fun ColumnScope.FailedScreen(code: Int?) {
        val message = errorMessage(code, LocalContext.current.resources)
        val ignorable = ignorableError(code)
        ResultScreenTemplate(message, Icons.Default.Warning, hasButton = ignorable, autoJump = false)
    }

    private fun errorMessage(code: Int?, resources: Resources): String {
        return when (code) {
            MigrationManager.CODE_NO_ACTION -> "No Need to Migrate!"
            MigrationManager.CODE_WARNING   ->
                "${resources.getString(R.string.version_migration_hint_too_old)}\n${resources.getString(R.string.version_migration_hint_suggest_to_wipe_data)}"

            MigrationManager.CODE_FORBIDDEN ->
                "${resources.getString(R.string.version_migration_hint_too_old)}\n${resources.getString(R.string.version_migration_hint_required_to_wipe_data)}"

            else                            -> resources.getString(R.string.failed)
        }
    }

    private fun ignorableError(code: Int?): Boolean {
        return when (code) {
            MigrationManager.CODE_FORBIDDEN -> false
            else                            -> true
        }
        return true
    }

    @Composable
    private fun ColumnScope.ResultScreenTemplate(
        text: String,
        icon: ImageVector,
        hasButton: Boolean = true,
        autoJump: Boolean = false,
    ) {
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
        if (hasButton) TextButton(
            ::gotoMainActivity,
            Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                stringResource(android.R.string.ok),
                Modifier.padding(4.dp)
            )
        }
        if (autoJump) SideEffect {
            jumpToMainActivity()
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


    private fun migrateImpl() {
        lifecycleScope.launch {
            delay(1000)
            withContext(Dispatchers.IO) {
                migrationResultFlow.emit(
                    MigrationManager.migrate(this@MigrationActivity)
                )
                isCompletedFlow.value = true
            }
        }
    }

    private fun jumpToMainActivity() {
        lifecycleScope.launch {
            delay(1000)
            gotoMainActivity()
        }
    }

    private fun gotoMainActivity() {
        startActivity(MainActivity.launchingIntent(this))
        finish()
    }

    companion object {
        private val colorProcess = Color(red = 33, green = 121, blue = 158)
        private val colorDone = Color(red = 5, green = 141, blue = 124, alpha = 255)
    }
}

