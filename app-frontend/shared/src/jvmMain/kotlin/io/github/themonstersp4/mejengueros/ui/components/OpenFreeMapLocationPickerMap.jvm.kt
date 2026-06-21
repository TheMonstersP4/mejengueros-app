package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

@Composable
internal actual fun PlatformOpenFreeMapLocationPickerMap(
    draftLocation: SelectedLocation,
    onDraftLocationChange: (SelectedLocation) -> Unit,
    onMapStateChange: (MejenguerosLocationMapState) -> Unit,
    modifier: Modifier,
) {
  val draftLocationPresenter =
      remember(onDraftLocationChange) {
        OpenFreeMapDraftLocationPresenter(
            initialDraftLocation = draftLocation,
            onDraftLocationChange = onDraftLocationChange,
        )
      }
  val cameraState =
      rememberCameraState(
          firstPosition =
              CameraPosition(
                  target = draftLocation.toMapPosition(),
                  zoom = 15.5,
                  bearing = 0.0,
                  tilt = 0.0,
              )
      )

  LaunchedEffect(Unit) { onMapStateChange(MejenguerosLocationMapState.Loading) }

  LaunchedEffect(draftLocation.latitude, draftLocation.longitude) {
    draftLocationPresenter.onDraftLocationSynced(draftLocation)
    val currentTarget = cameraState.position.target
    if (
        abs(currentTarget.latitude - draftLocation.latitude) > 0.00001 ||
            abs(currentTarget.longitude - draftLocation.longitude) > 0.00001
    ) {
      cameraState.position = cameraState.position.copy(target = draftLocation.toMapPosition())
    }
  }

  LaunchedEffect(cameraState) {
    snapshotFlow { cameraState.position.target }
        .distinctUntilChanged()
        .collect { position ->
          draftLocationPresenter.onCameraTargetChanged(
              latitude = position.latitude,
              longitude = position.longitude,
          )
        }
  }

  MaplibreMap(
      modifier = modifier,
      baseStyle = BaseStyle.Uri(OpenFreeMapLibertyStyleUrl),
      cameraState = cameraState,
      onMapClick = { position, _ ->
        cameraState.position = cameraState.position.copy(target = position)
        draftLocationPresenter.onMapClicked(
            latitude = position.latitude,
            longitude = position.longitude,
        )
        ClickResult.Consume
      },
      onMapLoadFinished = { onMapStateChange(MejenguerosLocationMapState.Ready) },
      onMapLoadFailed = { _ -> onMapStateChange(MejenguerosLocationMapState.Error) },
  )
}

private fun SelectedLocation.toMapPosition(): Position =
    Position(latitude = latitude, longitude = longitude)
