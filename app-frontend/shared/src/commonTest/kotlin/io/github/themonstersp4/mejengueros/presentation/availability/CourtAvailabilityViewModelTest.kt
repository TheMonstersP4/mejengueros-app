package io.github.themonstersp4.mejengueros.presentation.availability

import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityConfig
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityContext
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilityWeekday
import io.github.themonstersp4.mejengueros.domain.repository.ICourtAvailabilityRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class CourtAvailabilityViewModelTest {
  @Test
  fun loadUsesRealCourtContextAndExistingAvailability() = runTest {
    val repository = FakeCourtAvailabilityRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        CourtAvailabilityViewModel(
            courtId = "court-id",
            initialCourtName = "Cancha 1",
            initialComplexName = "Mejengas CR",
            repository = repository,
            coroutineScope = scope,
        )

    advanceUntilIdle()

    assertEquals(listOf("court-id"), repository.loadedCourtIds)
    assertEquals("Cancha 1", viewModel.uiState.value.courtName)
    assertEquals("Mejengas CR", viewModel.uiState.value.complexName)
    assertEquals("Disponibilidad", viewModel.uiState.value.appBarTitle)
    assertEquals(
        setOf(CourtAvailabilityWeekday.MONDAY, CourtAvailabilityWeekday.WEDNESDAY),
        viewModel.uiState.value.selectedDays,
    )
    assertEquals(listOf("06:00", "07:00", "08:00"), viewModel.uiState.value.previewSlots)
    scope.cancel()
  }

  @Test
  fun saveRequiresAtLeastOneWeekday() = runTest {
    val repository = FakeCourtAvailabilityRepository(initialAvailability = null)
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        CourtAvailabilityViewModel(
            courtId = "court-id",
            initialCourtName = "Cancha 1",
            initialComplexName = "Mejengas CR",
            repository = repository,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    viewModel.save()
    advanceUntilIdle()

    assertEquals("Elegí al menos un día disponible.", viewModel.uiState.value.errorMessage)
    assertEquals(emptyList(), repository.savedConfigs)
    scope.cancel()
  }

  @Test
  fun savePersistsAvailabilityAndShowsConfirmationMessage() = runTest {
    val repository = FakeCourtAvailabilityRepository(initialAvailability = null)
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        CourtAvailabilityViewModel(
            courtId = "court-id",
            initialCourtName = "Cancha 1",
            initialComplexName = "Mejengas CR",
            repository = repository,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    viewModel.toggleDay(CourtAvailabilityWeekday.MONDAY)
    viewModel.toggleDay(CourtAvailabilityWeekday.FRIDAY)
    viewModel.updateStartTime("07:00")
    viewModel.updateEndTime("10:00")
    viewModel.save()
    advanceUntilIdle()

    assertEquals(
        listOf(
            CourtAvailabilityConfig(
                days = listOf(CourtAvailabilityWeekday.MONDAY, CourtAvailabilityWeekday.FRIDAY),
                startTime = "07:00",
                endTime = "10:00",
            )
        ),
        repository.savedConfigs,
    )
    assertEquals(
        "Tu cancha ya tiene una disponibilidad base para recibir reservas.",
        viewModel.uiState.value.successMessage,
    )
    assertNull(viewModel.uiState.value.errorMessage)
    assertFalse(viewModel.uiState.value.isSaving)
    scope.cancel()
  }

  @Test
  fun acknowledgeSuccessClearsSuccessMessage() = runTest {
    val repository = FakeCourtAvailabilityRepository(initialAvailability = null)
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        CourtAvailabilityViewModel(
            courtId = "court-id",
            initialCourtName = "Cancha 1",
            initialComplexName = "Mejengas CR",
            repository = repository,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    viewModel.toggleDay(CourtAvailabilityWeekday.MONDAY)
    viewModel.save()
    advanceUntilIdle()
    viewModel.acknowledgeSuccess()

    assertNull(viewModel.uiState.value.successMessage)
    scope.cancel()
  }

  @Test
  fun saveMapsApiFailuresIntoUserFriendlyMessage() = runTest {
    val repository =
        FakeCourtAvailabilityRepository(
            initialAvailability = null,
            saveError = AppApiException(statusCode = 403, message = "Forbidden"),
        )
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        CourtAvailabilityViewModel(
            courtId = "court-id",
            initialCourtName = "Cancha 1",
            initialComplexName = "Mejengas CR",
            repository = repository,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    viewModel.toggleDay(CourtAvailabilityWeekday.MONDAY)
    viewModel.save()
    advanceUntilIdle()

    assertEquals(
        "No tenés permisos para configurar la disponibilidad de esta cancha.",
        viewModel.uiState.value.errorMessage,
    )
    scope.cancel()
  }

  @Test
  fun loadMapsUnauthorizedAndForbiddenApiFailuresIntoUserFriendlyMessage() = runTest {
    listOf(401, 403).forEach { statusCode ->
      val repository =
          FakeCourtAvailabilityRepository(
              loadError = AppApiException(statusCode = statusCode, message = "Forbidden"),
          )
      val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
      val viewModel =
          CourtAvailabilityViewModel(
              courtId = "court-id",
              initialCourtName = "Cancha 1",
              initialComplexName = "Mejengas CR",
              repository = repository,
              coroutineScope = scope,
          )

      advanceUntilIdle()

      assertEquals(
          "No tenés permisos para ver la disponibilidad de esta cancha.",
          viewModel.uiState.value.errorMessage,
      )
      assertFalse(viewModel.uiState.value.isLoading)
      scope.cancel()
    }
  }
}

private class FakeCourtAvailabilityRepository(
    initialAvailability: CourtAvailabilityConfig? =
        CourtAvailabilityConfig(
            days = listOf(CourtAvailabilityWeekday.MONDAY, CourtAvailabilityWeekday.WEDNESDAY),
            startTime = "06:00",
            endTime = "09:00",
        ),
    private val loadError: Throwable? = null,
    private val saveError: Throwable? = null,
) : ICourtAvailabilityRepository {
  val loadedCourtIds = mutableListOf<String>()
  val savedConfigs = mutableListOf<CourtAvailabilityConfig>()
  private var currentContext =
      CourtAvailabilityContext(
          courtId = "court-id",
          courtName = "Cancha 1",
          complexName = "Mejengas CR",
          availability = initialAvailability,
      )

  override suspend fun getCourtAvailability(courtId: String): CourtAvailabilityContext {
    loadedCourtIds.add(courtId)
    loadError?.let { throw it }
    return currentContext
  }

  override suspend fun saveCourtAvailability(
      courtId: String,
      availability: CourtAvailabilityConfig,
  ): CourtAvailabilityContext {
    saveError?.let { throw it }
    savedConfigs.add(availability)
    currentContext = currentContext.copy(availability = availability)
    return currentContext
  }
}
