package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

@Composable
internal actual fun PlatformOpenFreeMapStaticMap(
    location: SelectedLocation,
    modifier: Modifier,
) {
  val cameraState =
      rememberCameraState(
          firstPosition =
              CameraPosition(
                  target = Position(latitude = location.latitude, longitude = location.longitude),
                  zoom = 15.5,
                  bearing = 0.0,
                  tilt = 0.0,
              )
      )

  MaplibreMap(
      modifier = modifier,
      baseStyle = BaseStyle.Uri(OpenFreeMapLibertyStyleUrl),
      cameraState = cameraState,
      options = MapOptions(gestureOptions = GestureOptions.AllDisabled),
  )
}
