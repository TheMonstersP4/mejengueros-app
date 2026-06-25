package io.github.themonstersp4.mejengueros.screens.complexes

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexStep
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexUiState
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CreateComplexScreenBehaviorTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun nextButtonStaysDisabledUntilComplexStepRequiredFieldsAreComplete() {
    val host = composeRule.setCreateComplexScreenContent()

    composeRule.nextButton().assertIsNotEnabled()
    composeRule.complexNameField().performTextInput("North Sports Center")
    composeRule.addressField().performTextInput("123 Main Street")
    composeRule.nextButton().assertIsNotEnabled()
    host.selectProvince(composeRule, "province-1")
    composeRule.nextButton().assertIsNotEnabled()
    host.selectCanton(composeRule, "canton-1")

    composeRule.nextButton().assertIsEnabled()
  }

  @Test
  fun changingProvinceClearsSelectedCantonAndDisablesNextUntilNewCantonIsChosen() {
    val host =
        composeRule.setCreateComplexScreenContent(
            initialState =
                defaultUiState()
                    .copy(
                        complexName = "North Sports Center",
                        complexAddress = "123 Main Street",
                    )
        )

    host.selectProvince(composeRule, "province-1")
    host.selectCanton(composeRule, "canton-1")
    composeRule.nextButton().assertIsEnabled()

    host.selectProvince(composeRule, "province-2")

    composeRule.nextButton().assertIsNotEnabled()
    host.selectCanton(composeRule, "canton-2")
    composeRule.nextButton().assertIsEnabled()
  }

  @Test
  fun submitRequiresCourtServiceSelectionAndRemovesOldOwnerProvisioningCopy() {
    var submits = 0
    val host =
        composeRule.setCreateComplexScreenContent(
            initialState =
                defaultUiState()
                    .copy(
                        currentStep = CreateComplexStep.FirstCourt,
                        complexName = "North Sports Center",
                        complexAddress = "123 Main Street",
                        selectedProvinceId = "province-1",
                        selectedCantonId = "canton-1",
                        firstCourtName = "Court A",
                        cantons = cantonsByProvince.getValue("province-1"),
                    ),
            onSubmit = { submits += 1 },
        )

    composeRule.submitButton().assertIsNotEnabled()
    composeRule
        .onAllNodes(
            hasText(
                "Si tu usuario todavía no tiene el rol OWNER local, el sistema mostrará el bloqueo para que puedas pedir la provisión demo."
            )
        )
        .assertCountEquals(0)
    host.toggleCourtService(composeRule, "court-service-id")
    composeRule.submitButton().performScrollTo().assertIsEnabled().performTouchInput { click() }

    composeRule.runOnIdle { assertEquals(1, submits) }
  }

  @Test
  fun catalogFailureShowsRetryCallToAction() {
    var retries = 0

    composeRule.setCreateComplexScreenContent(
        initialState =
            CreateComplexUiState(
                loadErrorMessage = "No pudimos cargar los catálogos del complejo.",
                isLoadingCatalogs = false,
            ),
        onRetryCatalogs = { retries += 1 },
    )

    composeRule
        .onNodeWithTag("create_complex_retry_catalogs_button", useUnmergedTree = true)
        .assertIsEnabled()
        .performScrollTo()
        .performTouchInput { click() }

    composeRule.runOnIdle { assertEquals(1, retries) }
  }

  @Test
  fun cantonFailureShowsRetryCallToAction() {
    var retries = 0

    composeRule.setCreateComplexScreenContent(
        initialState =
            defaultUiState()
                .copy(
                    selectedProvinceId = "province-1",
                    loadErrorMessage = "No pudimos cargar los cantones.",
                    hasCantonLoadFailure = true,
                    isLoadingCantons = false,
                ),
        onRetryCantons = { retries += 1 },
    )

    composeRule
        .onNodeWithTag("create_complex_retry_cantons_button", useUnmergedTree = true)
        .assertIsEnabled()
        .performScrollTo()
        .performTouchInput { click() }

    composeRule.runOnIdle { assertEquals(1, retries) }
  }

  private fun ComposeContentTestRule.setCreateComplexScreenContent(
      initialState: CreateComplexUiState = defaultUiState(),
      onRetryCatalogs: () -> Unit = {},
      onRetryCantons: () -> Unit = {},
      onSubmit: () -> Unit = {},
  ): CreateComplexScreenTestHost {
    val host = CreateComplexScreenTestHost()
    setContent {
      var localState by remember { mutableStateOf(initialState) }
      host.selectProvince = { provinceId ->
        localState =
            localState.copy(
                selectedProvinceId = provinceId,
                selectedCantonId = null,
                cantons = cantonsByProvince.getValue(provinceId),
            )
      }
      host.selectCanton = { cantonId -> localState = localState.copy(selectedCantonId = cantonId) }
      host.toggleCourtService = { serviceId ->
        localState =
            localState.copy(
                selectedCourtServiceIds = toggle(localState.selectedCourtServiceIds, serviceId)
            )
      }

      MejenguerosTheme {
        CreateComplexScreen(
            state = localState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            onRetryCatalogs = onRetryCatalogs,
            onRetryCantons = onRetryCantons,
            onComplexNameChange = { localState = localState.copy(complexName = it) },
            onProvinceSelected = host.selectProvince,
            onCantonSelected = host.selectCanton,
            onComplexAddressChange = { localState = localState.copy(complexAddress = it) },
            onOpenLocationPicker = {},
            onClearLocation = { localState = localState.copy(latitude = null, longitude = null) },
            onToggleComplexService = { serviceId ->
              localState =
                  localState.copy(
                      selectedComplexServiceIds =
                          toggle(localState.selectedComplexServiceIds, serviceId)
                  )
            },
            onFirstCourtNameChange = { localState = localState.copy(firstCourtName = it) },
            onToggleCourtService = host.toggleCourtService,
            onNext = { localState = localState.copy(currentStep = CreateComplexStep.FirstCourt) },
            onBack = { localState = localState.copy(currentStep = CreateComplexStep.Complex) },
            onSubmit = onSubmit,
        )
      }
    }
    return host
  }

  private fun ComposeContentTestRule.complexNameField() =
      onNode(hasSetTextAction() and hasContentDescription("Nombre del complejo"))

  private fun ComposeContentTestRule.addressField() =
      onNode(hasSetTextAction() and hasContentDescription("Dirección"))

  private fun ComposeContentTestRule.nextButton() =
      onNodeWithTag("create_complex_next_button", useUnmergedTree = true)

  private fun ComposeContentTestRule.submitButton() =
      onNodeWithTag("create_complex_submit_button", useUnmergedTree = true)

  private class CreateComplexScreenTestHost {
    lateinit var selectProvince: (String) -> Unit
    lateinit var selectCanton: (String) -> Unit
    lateinit var toggleCourtService: (String) -> Unit

    fun selectProvince(rule: ComposeContentTestRule, provinceId: String) {
      rule.runOnIdle { selectProvince(provinceId) }
    }

    fun selectCanton(rule: ComposeContentTestRule, cantonId: String) {
      rule.runOnIdle { selectCanton(cantonId) }
    }

    fun toggleCourtService(rule: ComposeContentTestRule, serviceId: String) {
      rule.runOnIdle { toggleCourtService(serviceId) }
    }
  }

  private companion object {
    val provinces =
        listOf(
            Province(id = "province-1", code = "SJ", name = "San José"),
            Province(id = "province-2", code = "AL", name = "Alajuela"),
        )
    val cantonsByProvince =
        mapOf(
            "province-1" to
                listOf(
                    Canton(
                        id = "canton-1",
                        provinceId = "province-1",
                        code = "SJ-ESC",
                        name = "Escazú",
                    )
                ),
            "province-2" to
                listOf(
                    Canton(
                        id = "canton-2",
                        provinceId = "province-2",
                        code = "AL-GRE",
                        name = "Grecia",
                    )
                ),
        )
    val complexServices =
        listOf(
            ServiceCatalogItem(
                id = "complex-service-id",
                name = "Parking",
                scope = ServiceScope.COMPLEX,
            )
        )
    val courtServices =
        listOf(
            ServiceCatalogItem(
                id = "court-service-id",
                name = "Synthetic Grass",
                scope = ServiceScope.COURT,
            )
        )

    fun defaultUiState() =
        CreateComplexUiState(
            provinces = provinces,
            complexServices = complexServices,
            courtServices = courtServices,
            isLoadingCatalogs = false,
        )

    fun toggle(values: List<String>, target: String): List<String> =
        if (target in values) values.filterNot { it == target } else values + target
  }
}
