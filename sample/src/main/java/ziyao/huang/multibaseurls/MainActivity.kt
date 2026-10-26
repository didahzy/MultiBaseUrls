package ziyao.huang.multibaseurls

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ziyao.huang.multibaseurls.ui.theme.MultiBaseUrlsTheme

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MultiBaseUrlsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SampleScreen(
                        modifier = Modifier
                            .padding(paddingValues = innerPadding)
                            .consumeWindowInsets(paddingValues = innerPadding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        state = viewModel.uiState,
                        onRun = viewModel::runExample,
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleScreen(
    modifier: Modifier = Modifier,
    state: UiState,
    onRun: (Example) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "MultiBaseUrls Sample", style = MaterialTheme.typography.titleLarge)

        Text(
            text = "Original baseUrl: https://jsonplaceholder.typicode.com/",
            style = MaterialTheme.typography.bodySmall,
        )

        Example.entries.forEach { example ->
            ElevatedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onRun(example) },
            ) {
                Text(example.label)
            }
        }

        when (val current = state) {
            UiState.Idle -> Text("Tap a button to try it", style = MaterialTheme.typography.bodyMedium)
            UiState.Loading -> CircularProgressIndicator()
            is UiState.Success -> Card {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Actual request URL:", style = MaterialTheme.typography.labelMedium)
                    Text(current.requestUrl, style = MaterialTheme.typography.bodySmall)
                    Text("Response:", style = MaterialTheme.typography.labelMedium)
                    Text(current.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            is UiState.Error -> Text(
                text = "Error: ${current.message}",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
