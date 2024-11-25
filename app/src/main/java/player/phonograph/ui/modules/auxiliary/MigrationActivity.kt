package player.phonograph.ui.modules.auxiliary

import player.phonograph.R
import player.phonograph.mechanism.migrate.MigrationManager
import player.phonograph.ui.basis.ComposeActivity
import player.phonograph.ui.compose.PhonographTheme
import player.phonograph.ui.compose.components.StatusBarStub
import player.phonograph.ui.modules.main.MainActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
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
                StatusBarStub()
                Scaffold(
                    Modifier.systemBarsPadding(),
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.version_migration)) },
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

    @Composable
    private fun ColumnScope.ResultScreen() {
        SuccessScreen()
    }

    @Composable
    private fun ColumnScope.SuccessScreen() {
        ResultScreenTemplate(stringResource(R.string.success), Icons.Default.Done, hasButton = true, autoJump = true)
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

