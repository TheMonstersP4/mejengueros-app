package example.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LaunchListRoute(
    viewModel: LaunchListViewModel = koinViewModel(),
    onLaunchClick: (LaunchUiModel) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchListScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onLaunchClick = onLaunchClick,
    )
}

@Composable
fun LaunchListScreen(
    state: LaunchListUiState,
    onRefresh: () -> Unit,
    onLaunchClick: (LaunchUiModel) -> Unit,
) {
    // UI only: render immutable state and emit callbacks.
    // Private visual state can stay local when no parent/business logic needs it.
}
