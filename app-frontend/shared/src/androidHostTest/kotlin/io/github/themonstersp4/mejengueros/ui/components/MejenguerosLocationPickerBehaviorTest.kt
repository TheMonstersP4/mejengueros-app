package io.github.themonstersp4.mejengueros.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.screens.kit.ComponentKitDemoLocationPickerCenter
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MejenguerosLocationPickerBehaviorTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun confirmUpdatesSelectedStateAfterChangingDraftLocation() {
    val updatedLocation = SelectedLocation(latitude = 10.12345, longitude = -84.54321)
    val updatedCoordinatesText = "Coordenadas: ${updatedLocation.toCoordinatePairText()}"
    val pickerCoordinatesText = updatedLocation.toCoordinatePairText()
    val fakeMapController = FakeMapController(updatedLocation = updatedLocation)

    composeRule.setContent {
      MejenguerosTheme {
        LocationPickerFlowHost(
            initialSelectedLocation = null,
            mapUpdatedLocation = updatedLocation,
            fakeMapController = fakeMapController,
        )
      }
    }

    composeRule.actionButton("Seleccionar ubicación").performClick()
    composeRule.waitForIdle()
    composeRule.runOnIdle { fakeMapController.moveDraftLocation() }
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Punto seleccionado").assertExists()
    composeRule.onAllNodesWithText(pickerCoordinatesText).assertCountEquals(2)
    composeRule.actionButton("Usar esta ubicación").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Ubicación seleccionada").assertExists()
    composeRule.onNodeWithText(updatedCoordinatesText).assertExists()
    composeRule.onNodeWithText("Cambiar ubicación").assertExists()
  }

  @Test
  fun compactFieldWithoutSelectionOpensPickerCallback() {
    var openPickerCalls = 0

    composeRule.setContent {
      MejenguerosTheme {
        MejenguerosLocationField(
            selectedLocation = null,
            onOpenPicker = { openPickerCalls += 1 },
        )
      }
    }

    composeRule.onNodeWithText("Sin ubicación seleccionada").assertExists()
    composeRule.onNodeWithText("Todavía no elegiste un punto en el mapa.").assertExists()
    composeRule.actionButton("Seleccionar ubicación").performClick()

    composeRule.runOnIdle { kotlin.test.assertEquals(1, openPickerCalls) }
  }

  @Test
  fun compactFieldWithSelectionShowsCoordinatesAndChangeAction() {
    val selectedLocation = SelectedLocation(latitude = 9.93333, longitude = -84.08888)

    composeRule.setContent {
      MejenguerosTheme {
        MejenguerosLocationField(
            selectedLocation = selectedLocation,
            onOpenPicker = {},
        )
      }
    }

    composeRule.onNodeWithText("Ubicación seleccionada").assertExists()
    composeRule
        .onNodeWithText("Coordenadas: ${selectedLocation.toCoordinatePairText()}")
        .assertExists()
    composeRule.onNodeWithText("Cambiar ubicación").assertExists()
  }

  @Test
  fun cancelDismissPreservesPreviousSelectedLocation() {
    val previousLocation = SelectedLocation(latitude = 9.93333, longitude = -84.08888)
    val updatedLocation = SelectedLocation(latitude = 10.12345, longitude = -84.54321)
    val previousCoordinatesText = "Coordenadas: ${previousLocation.toCoordinatePairText()}"
    val updatedCoordinatesText = "Coordenadas: ${updatedLocation.toCoordinatePairText()}"
    val fakeMapController = FakeMapController(updatedLocation = updatedLocation)

    composeRule.setContent {
      MejenguerosTheme {
        LocationPickerFlowHost(
            initialSelectedLocation = previousLocation,
            mapUpdatedLocation = updatedLocation,
            fakeMapController = fakeMapController,
        )
      }
    }

    composeRule.actionButton("Cambiar ubicación").performClick()
    composeRule.waitForIdle()
    composeRule.runOnIdle { fakeMapController.moveDraftLocation() }
    composeRule.waitForIdle()
    composeRule.actionButton("Cancelar").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Ubicación seleccionada").assertExists()
    composeRule.onNodeWithText(previousCoordinatesText).assertExists()
    composeRule.onAllNodesWithText(updatedCoordinatesText).assertCountEquals(0)
    composeRule.onNodeWithText("Cambiar ubicación").assertExists()
  }

  @Test
  fun platformBackDismissesPickerWithoutChangingSelectedLocation() {
    val previousLocation = SelectedLocation(latitude = 9.93333, longitude = -84.08888)
    val updatedLocation = SelectedLocation(latitude = 10.12345, longitude = -84.54321)
    val previousCoordinatesText = "Coordenadas: ${previousLocation.toCoordinatePairText()}"
    val updatedCoordinatesText = "Coordenadas: ${updatedLocation.toCoordinatePairText()}"
    val fakeMapController = FakeMapController(updatedLocation = updatedLocation)

    composeRule.setContent {
      MejenguerosTheme {
        LocationPickerFlowHost(
            initialSelectedLocation = previousLocation,
            mapUpdatedLocation = updatedLocation,
            fakeMapController = fakeMapController,
        )
      }
    }

    composeRule.actionButton("Cambiar ubicación").performClick()
    composeRule.waitForIdle()
    composeRule.runOnIdle { fakeMapController.moveDraftLocation() }
    composeRule.waitForIdle()
    composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("location_picker_overlay").assertDoesNotExist()
    composeRule.onNodeWithText("Ubicación seleccionada").assertExists()
    composeRule.onNodeWithText(previousCoordinatesText).assertExists()
    composeRule.onAllNodesWithText(updatedCoordinatesText).assertCountEquals(0)
  }

  @Test
  fun confirmUsesLatestDraftCoordinates() {
    val firstDraftLocation = SelectedLocation(latitude = 10.12345, longitude = -84.54321)
    val latestDraftLocation = SelectedLocation(latitude = 11.98765, longitude = -85.45678)
    val fakeMapController = FakeMapController(updatedLocation = firstDraftLocation)

    composeRule.setContent {
      MejenguerosTheme {
        LocationPickerFlowHost(
            initialSelectedLocation = null,
            mapUpdatedLocation = firstDraftLocation,
            fakeMapController = fakeMapController,
        )
      }
    }

    composeRule.actionButton("Seleccionar ubicación").performClick()
    composeRule.waitForIdle()
    composeRule.runOnIdle {
      fakeMapController.moveDraftLocation(firstDraftLocation)
      fakeMapController.moveDraftLocation(latestDraftLocation)
    }
    composeRule.waitForIdle()
    composeRule.actionButton("Usar esta ubicación").performClick()
    composeRule.waitForIdle()

    composeRule
        .onNodeWithText("Coordenadas: ${latestDraftLocation.toCoordinatePairText()}")
        .assertExists()
    composeRule
        .onAllNodesWithText("Coordenadas: ${firstDraftLocation.toCoordinatePairText()}")
        .assertCountEquals(0)
  }

  @Test
  fun overlayUsesProductionFocusedControls() {
    composeRule.setContent {
      MejenguerosTheme {
        MejenguerosLocationPickerScreen(
            state =
                MejenguerosLocationPickerState(
                    draftLocation = ComponentKitDemoLocationPickerCenter,
                    selectedLocation = null,
                ),
            actions =
                MejenguerosLocationPickerActions(
                    onDraftLocationChange = {},
                    onConfirm = {},
                    onDismiss = {},
                ),
            mapContent = { scope ->
              SideEffect { scope.onMapStateChange(MejenguerosLocationMapState.Ready) }
              Box(modifier = scope.modifier.testTag("fake_map"))
            },
        )
      }
    }

    composeRule.onNodeWithTag("location_picker_top_controls").assertExists()
    composeRule.onNodeWithTag("location_picker_bottom_controls").assertExists()
    composeRule
        .onNodeWithTag("location_picker_overlay")
        .assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.PaneTitle,
                "Location picker",
            )
        )
    composeRule.onNodeWithText("Elegí la ubicación").assertExists()
    composeRule.onNodeWithText("Punto seleccionado").assertExists()
    composeRule
        .onNodeWithText(ComponentKitDemoLocationPickerCenter.toCoordinatePairText())
        .assertExists()
    composeRule.onAllNodesWithText("Borrador actual").assertCountEquals(0)
    composeRule.onAllNodesWithText("OpenFreeMap · Liberty").assertCountEquals(0)
    composeRule.onAllNodesWithText("OSM · demo/MVP sin SLA garantizado").assertCountEquals(0)
  }

  @Test
  fun unsupportedFallbackShowsUserVisibleContingencyCopy() {
    composeRule.setFallbackContent(mapState = MejenguerosLocationMapState.Unsupported)

    composeRule.onNodeWithText("Mapa no disponible").assertExists()
    composeRule
        .onNodeWithText(
            "El mapa no está disponible en esta plataforma por ahora. Podés continuar con las coordenadas actuales: ${ComponentKitDemoLocationPickerCenter.toCoordinatePairText()}"
        )
        .assertExists()
  }

  @Test
  fun errorFallbackShowsUserVisibleRecoveryCopy() {
    composeRule.setFallbackContent(mapState = MejenguerosLocationMapState.Error)

    composeRule.onNodeWithText("No se pudo iniciar el mapa").assertExists()
    composeRule
        .onNodeWithText("No pudimos cargar el mapa en este momento. Intentá de nuevo más tarde.")
        .assertExists()
  }

  private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.setFallbackContent(
      mapState: MejenguerosLocationMapState,
  ) {
    setContent {
      MejenguerosTheme {
        MejenguerosLocationPickerScreen(
            state =
                MejenguerosLocationPickerState(
                    draftLocation = ComponentKitDemoLocationPickerCenter,
                    selectedLocation = null,
                ),
            actions =
                MejenguerosLocationPickerActions(
                    onDraftLocationChange = {},
                    onConfirm = {},
                    onDismiss = {},
                ),
            mapContent = { scope ->
              SideEffect { scope.onMapStateChange(mapState) }
              DefaultLocationPickerMapPlaceholder(
                  draftLocation = scope.draftLocation,
                  mapState = mapState,
                  modifier = scope.modifier,
              )
            },
        )
      }
    }
  }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.actionButton(text: String) =
    onNode(hasText(text) and hasClickAction())

@Composable
private fun LocationPickerFlowHost(
    initialSelectedLocation: SelectedLocation?,
    mapUpdatedLocation: SelectedLocation,
    fakeMapController: FakeMapController = FakeMapController(updatedLocation = mapUpdatedLocation),
) {
  var selectedLocation by remember { mutableStateOf(initialSelectedLocation) }
  var draftLocation by remember {
    mutableStateOf(initialSelectedLocation ?: ComponentKitDemoLocationPickerCenter)
  }
  var isPickerOpen by remember { mutableStateOf(false) }

  MejenguerosLocationField(
      selectedLocation = selectedLocation,
      onOpenPicker = {
        draftLocation = selectedLocation ?: ComponentKitDemoLocationPickerCenter
        isPickerOpen = true
      },
  )

  if (isPickerOpen) {
    MejenguerosLocationPickerScreen(
        state =
            MejenguerosLocationPickerState(
                draftLocation = draftLocation,
                selectedLocation = selectedLocation,
            ),
        actions =
            MejenguerosLocationPickerActions(
                onDraftLocationChange = { draftLocation = it },
                onConfirm = {
                  selectedLocation = it
                  draftLocation = it
                  isPickerOpen = false
                },
                onDismiss = { isPickerOpen = false },
            ),
        mapContent = { scope -> FakeMapContent(scope = scope, controller = fakeMapController) },
    )
  }
}

private class FakeMapController(
    private val updatedLocation: SelectedLocation,
) {
  private var onDraftLocationChange: ((SelectedLocation) -> Unit)? = null

  fun bind(onDraftLocationChange: (SelectedLocation) -> Unit) {
    this.onDraftLocationChange = onDraftLocationChange
  }

  fun moveDraftLocation() {
    onDraftLocationChange?.invoke(updatedLocation)
  }

  fun moveDraftLocation(location: SelectedLocation) {
    onDraftLocationChange?.invoke(location)
  }
}

@Composable
private fun FakeMapContent(
    scope: MejenguerosLocationPickerMapScope,
    controller: FakeMapController,
) {
  SideEffect {
    scope.onMapStateChange(MejenguerosLocationMapState.Ready)
    controller.bind(scope.onDraftLocationChange)
  }

  Box(modifier = scope.modifier, contentAlignment = Alignment.Center) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = scope.draftLocation.toCoordinatePairText())
    }
  }
}
