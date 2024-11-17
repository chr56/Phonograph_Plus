package player.phonograph.ui.modules.auxiliary

import player.phonograph.R
import player.phonograph.mechanism.migrate.MigrationManager
import player.phonograph.ui.basis.ComposeActivity
import player.phonograph.ui.compose.PhonographTheme
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

    private val isCompleteFlow = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isComplete by isCompleteFlow.collectAsState()
            val color = remember { derivedStateOf { if (isComplete) colorDone else colorProcess } }
            PhonographTheme(color) {
                Scaffold(
                    Modifier.systemBarsPadding(),
                    topBar = {
                        TopAppBar(title = { Text(stringResource(R.string.version_migration)) })
                    }
                ) {
                    Column(Modifier.padding(it)) {
                        ProgressScreen(isComplete)
                    }
                    SideEffect {
                        migrateImpl()
                    }
                }
            }
        }
    }

    @Composable
    private fun ColumnScope.ProgressScreen(done: Boolean) {
        if (done) {
            Image(
                Icons.Default.Done, stringResource(R.string.success),
                Modifier
                    .padding(56.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                stringResource(R.string.success),
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
            SideEffect {
                jumpToMainActivity()
            }
            TextButton(
                ::gotoMainActivity,
                Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    stringResource(android.R.string.ok),
                    Modifier.padding(4.dp)
                )
            }
        } else {
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
    }

    private fun migrateImpl() {
        lifecycleScope.launch {
            delay(1000)
            withContext(Dispatchers.IO) {
                MigrationManager.migrate(this@MigrationActivity)
                isCompleteFlow.value = true
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

