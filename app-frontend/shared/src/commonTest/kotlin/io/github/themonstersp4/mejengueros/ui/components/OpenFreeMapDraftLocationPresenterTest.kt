package io.github.themonstersp4.mejengueros.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenFreeMapDraftLocationPresenterTest {

  @Test
  fun cameraTargetChangeMapsToSelectedLocationAndUpdatesDraft() {
    val updates = mutableListOf<SelectedLocation>()
    val presenter =
        OpenFreeMapDraftLocationPresenter(
            initialDraftLocation = SelectedLocation(latitude = 9.93510, longitude = -84.09110),
            onDraftLocationChange = updates::add,
        )

    presenter.onCameraTargetChanged(latitude = 10.12345, longitude = -84.54321)

    assertEquals(
        listOf(SelectedLocation(latitude = 10.12345, longitude = -84.54321)),
        updates,
    )
  }

  @Test
  fun mapClickMapsToSelectedLocationAndUpdatesDraft() {
    val updates = mutableListOf<SelectedLocation>()
    val presenter =
        OpenFreeMapDraftLocationPresenter(
            initialDraftLocation = SelectedLocation(latitude = 9.93510, longitude = -84.09110),
            onDraftLocationChange = updates::add,
        )

    presenter.onMapClicked(latitude = 11.11111, longitude = -85.22222)

    assertEquals(
        listOf(SelectedLocation(latitude = 11.11111, longitude = -85.22222)),
        updates,
    )
  }

  @Test
  fun duplicateProviderPositionsDoNotTriggerExtraDraftUpdates() {
    val updates = mutableListOf<SelectedLocation>()
    val location = SelectedLocation(latitude = 10.12345, longitude = -84.54321)
    val presenter =
        OpenFreeMapDraftLocationPresenter(
            initialDraftLocation = SelectedLocation(latitude = 9.93510, longitude = -84.09110),
            onDraftLocationChange = updates::add,
        )

    presenter.onCameraTargetChanged(latitude = location.latitude, longitude = location.longitude)
    presenter.onMapClicked(latitude = location.latitude, longitude = location.longitude)
    presenter.onDraftLocationSynced(location)
    presenter.onCameraTargetChanged(latitude = location.latitude, longitude = location.longitude)

    assertEquals(listOf(location), updates)
  }

  @Test
  fun syncedLocationSkipsRepeatedCameraUpdatesUntilCoordinatesChange() {
    val updates = mutableListOf<SelectedLocation>()
    val syncedLocation = SelectedLocation(latitude = 9.93510, longitude = -84.09110)
    val changedLocation = SelectedLocation(latitude = 90.0, longitude = 180.0)
    val presenter =
        OpenFreeMapDraftLocationPresenter(
            initialDraftLocation = syncedLocation,
            onDraftLocationChange = updates::add,
        )

    presenter.onDraftLocationSynced(syncedLocation)
    presenter.onCameraTargetChanged(
        latitude = syncedLocation.latitude,
        longitude = syncedLocation.longitude,
    )
    presenter.onCameraTargetChanged(
        latitude = changedLocation.latitude,
        longitude = changedLocation.longitude,
    )
    presenter.onCameraTargetChanged(
        latitude = changedLocation.latitude,
        longitude = changedLocation.longitude,
    )

    assertEquals(listOf(changedLocation), updates)
  }

  @Test
  fun mapClickAcceptsCoordinateBoundaryValues() {
    val updates = mutableListOf<SelectedLocation>()
    val presenter =
        OpenFreeMapDraftLocationPresenter(
            initialDraftLocation = SelectedLocation(latitude = 0.0, longitude = 0.0),
            onDraftLocationChange = updates::add,
        )

    presenter.onMapClicked(latitude = -90.0, longitude = -180.0)
    presenter.onMapClicked(latitude = 90.0, longitude = 180.0)

    assertEquals(
        listOf(
            SelectedLocation(latitude = -90.0, longitude = -180.0),
            SelectedLocation(latitude = 90.0, longitude = 180.0),
        ),
        updates,
    )
  }
}
