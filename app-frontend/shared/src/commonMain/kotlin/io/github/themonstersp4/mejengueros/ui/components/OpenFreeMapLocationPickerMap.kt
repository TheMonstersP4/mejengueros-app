package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

internal const val OpenFreeMapLibertyStyleUrl = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun MejenguerosOpenFreeMapLocationPickerMap(scope: MejenguerosLocationPickerMapScope) {
  var mapState by remember { mutableStateOf(MejenguerosLocationMapState.Loading) }

  LaunchedEffect(mapState) { scope.onMapStateChange(mapState) }

  Box(modifier = scope.modifier.fillMaxSize()) {
    PlatformOpenFreeMapLocationPickerMap(
        draftLocation = scope.draftLocation,
        onDraftLocationChange = scope.onDraftLocationChange,
        onMapStateChange = { mapState = it },
        modifier = Modifier.fillMaxSize(),
    )

    if (
        mapState == MejenguerosLocationMapState.Unsupported ||
            mapState == MejenguerosLocationMapState.Error
    ) {
      DefaultLocationPickerMapPlaceholder(
          draftLocation = scope.draftLocation,
          mapState = mapState,
          modifier = Modifier.fillMaxSize(),
      )
    }

    if (
        mapState == MejenguerosLocationMapState.Loading ||
            mapState == MejenguerosLocationMapState.Ready
    ) {
      LocationPickerCrosshair(modifier = Modifier.align(Alignment.Center))
    }
  }
}

@Composable
private fun LocationPickerCrosshair(modifier: Modifier = Modifier) {
  Box(
      modifier = modifier,
      contentAlignment = Alignment.Center,
  ) {
    Box(
        modifier =
            Modifier.width(28.dp)
                .height(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
    )
    Box(
        modifier =
            Modifier.width(12.dp)
                .height(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
    )
    Box(
        modifier =
            Modifier.width(22.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
    Box(
        modifier =
            Modifier.width(2.dp)
                .height(22.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
  }
}

@Composable
internal expect fun PlatformOpenFreeMapLocationPickerMap(
    draftLocation: SelectedLocation,
    onDraftLocationChange: (SelectedLocation) -> Unit,
    onMapStateChange: (MejenguerosLocationMapState) -> Unit,
    modifier: Modifier = Modifier,
)
