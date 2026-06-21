package io.github.themonstersp4.mejengueros.ui.components

internal class OpenFreeMapDraftLocationPresenter(
    initialDraftLocation: SelectedLocation,
    private val onDraftLocationChange: (SelectedLocation) -> Unit,
) {
  private var lastLocation: SelectedLocation = initialDraftLocation

  fun onDraftLocationSynced(location: SelectedLocation) {
    lastLocation = location
  }

  fun onCameraTargetChanged(latitude: Double, longitude: Double) {
    emitIfChanged(SelectedLocation(latitude = latitude, longitude = longitude))
  }

  fun onMapClicked(latitude: Double, longitude: Double) {
    emitIfChanged(SelectedLocation(latitude = latitude, longitude = longitude))
  }

  private fun emitIfChanged(location: SelectedLocation) {
    if (location == lastLocation) {
      return
    }

    lastLocation = location
    onDraftLocationChange(location)
  }
}
