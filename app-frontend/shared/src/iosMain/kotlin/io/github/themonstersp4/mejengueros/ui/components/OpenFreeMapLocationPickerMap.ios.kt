package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@Composable
internal actual fun PlatformOpenFreeMapLocationPickerMap(
    draftLocation: SelectedLocation,
    onDraftLocationChange: (SelectedLocation) -> Unit,
    onMapStateChange: (MejenguerosLocationMapState) -> Unit,
    modifier: Modifier,
) {
  LaunchedEffect(Unit) { onMapStateChange(MejenguerosLocationMapState.Unsupported) }
  Box(modifier = modifier)
}
