package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun PlatformOpenFreeMapStaticMap(
    location: SelectedLocation,
    modifier: Modifier,
) {
  // iOS does not bundle the MapLibre renderer yet; the shared composable still
  // shows the pin over the surface background as a graceful fallback.
  Box(modifier = modifier)
}
