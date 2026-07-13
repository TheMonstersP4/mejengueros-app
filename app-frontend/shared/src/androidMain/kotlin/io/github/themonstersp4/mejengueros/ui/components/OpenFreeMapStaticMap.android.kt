package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.RenderOptions
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
      // TextureView (instead of the default SurfaceView) composites the map
      // inside the Compose window, so it respects the navigation cross-fade and
      // does not "punch through" over the previous screen during the back
      // transition.
      options =
          MapOptions(
              gestureOptions = GestureOptions.AllDisabled,
              renderOptions = RenderOptions(renderMode = RenderOptions.RenderMode.TextureView),
          ),
  )
}
