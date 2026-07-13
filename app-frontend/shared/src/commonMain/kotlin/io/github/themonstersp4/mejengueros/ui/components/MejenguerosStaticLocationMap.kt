package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Read-only map that shows where a court is located, centered on [location].
 *
 * The underlying platform map is not editable and its gestures are disabled, so the court always
 * stays under the centered pin. Platforms that cannot render a map render an empty surface, so the
 * pin over the surface background doubles as a graceful fallback and the section never appears
 * empty.
 *
 * [map] is injectable so tests can swap the native map (which needs a GL surface) for a lightweight
 * fake, mirroring the location picker.
 */
@Composable
fun MejenguerosStaticLocationMap(
    location: SelectedLocation,
    modifier: Modifier = Modifier,
    map: @Composable (SelectedLocation, Modifier) -> Unit = { mapLocation, mapModifier ->
      PlatformOpenFreeMapStaticMap(location = mapLocation, modifier = mapModifier)
    },
) {
  Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Box(contentAlignment = Alignment.Center) {
      map(location, Modifier.fillMaxSize())

      // Pin drawn on top of the map center. The icon is nudged up by half its
      // height so the tip of the marker sits exactly on the court coordinate.
      Icon(
          imageVector = Icons.Filled.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.offset(y = (-18).dp),
      )
    }
  }
}

/**
 * Platform map centered on [location] with editing and gestures disabled.
 *
 * Platforms without map support render an empty [Box] so the shared composable still shows the pin
 * over the surface background.
 */
@Composable
internal expect fun PlatformOpenFreeMapStaticMap(
    location: SelectedLocation,
    modifier: Modifier = Modifier,
)
