package ziyao.huang.multibaseurls

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/** The requests behind the three example buttons. */
enum class Example(val label: String) {
    DEFAULT("1. Untagged → default baseUrl"),
    BASE_URL("2. @BaseUrl(PokeApi) → switch environment"),
    IGNORE("3. @Ignore → force default baseUrl"),
}

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Success(val requestUrl: String, val summary: String) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel : ViewModel() {

    var uiState: UiState by mutableStateOf(UiState.Idle)
        private set

    fun runExample(example: Example) {
        uiState = UiState.Loading
        viewModelScope.launch {
            uiState = try {
                val response = when (example) {
                    Example.DEFAULT -> SampleApiService.api.defaultPost()
                    Example.BASE_URL -> SampleApiService.api.pokemon()
                    Example.IGNORE -> SampleApiService.api.ignoredPost()
                }
                UiState.Success(
                    requestUrl = response.raw().request.url.toString(),
                    summary = summarize(response.body()),
                )
            } catch (e: Exception) {
                UiState.Error(e.message ?: "unknown error")
            }
        }
    }

    private fun summarize(body: Any?): String = when (body) {
        is Post -> "${body.title}\n${body.body}"
        is Pokemon -> "#${body.id} ${body.name} — height: ${body.height}, weight: ${body.weight}"
        else -> "(empty body)"
    }
}
