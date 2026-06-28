package io.github.themonstersp4.mejengueros.presentation.mycomplex

import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class MyComplexViewModelTest {

  @Test
  fun refreshLoadsOwnerHubAndClearsLoadingState() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = MyComplexViewModel(repository, coroutineScope = scope)

    viewModel.refresh()
    advanceUntilIdle()

    assertEquals(1, repository.hubRequests)
    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.errorMessage)
    assertEquals(1, viewModel.uiState.value.complexes.size)
    assertEquals("complex-id", viewModel.uiState.value.complexes.single().id)
    assertEquals(
        CourtAvailabilitySetupStatus.CONFIGURED,
        viewModel.uiState.value.complexes.single().courts.first().availabilityStatus,
    )
  }

  @Test
  fun refreshExposesEmptyStateWhenOwnerHasNoComplexes() = runTest {
    val repository = FakeComplexRepository(hub = MyComplexHub(complexes = emptyList()))
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = MyComplexViewModel(repository, coroutineScope = scope)

    viewModel.refresh()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isEmpty)
    assertTrue(viewModel.uiState.value.complexes.isEmpty())
    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun refreshMapsApiFailuresToControlledErrorCopy() = runTest {
    val repository =
        FakeComplexRepository(
            failure = AppApiException(statusCode = 500, message = "Backend exploded")
        )
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = MyComplexViewModel(repository, coroutineScope = scope)

    viewModel.refresh()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(
        "No pudimos cargar tu hub de complejos. Intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )
    assertTrue(viewModel.uiState.value.complexes.isEmpty())
  }

  @Test
  fun refreshRetriesAfterFailureAndReplacesErrorWithLoadedData() = runTest {
    val repository =
        FakeComplexRepository(
            failures =
                ArrayDeque(
                    listOf(AppApiException(statusCode = 503, message = "Temporarily unavailable"))
                )
        )
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = MyComplexViewModel(repository, coroutineScope = scope)

    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(
        "No pudimos cargar tu hub de complejos. Intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )

    viewModel.refresh()
    advanceUntilIdle()

    assertEquals(2, repository.hubRequests)
    assertNull(viewModel.uiState.value.errorMessage)
    assertEquals(1, viewModel.uiState.value.complexes.size)
  }
}

private class FakeComplexRepository(
    private val hub: MyComplexHub =
        MyComplexHub(
            complexes =
                listOf(
                    MyComplexHubComplex(
                        id = "complex-id",
                        name = "North Sports Center",
                        address = "123 Main Street",
                        provinceId = "province-id",
                        cantonId = "canton-id",
                        latitude = 9.935,
                        longitude = -84.091,
                        status = "ACTIVE",
                        courts =
                            listOf(
                                MyComplexHubCourt(
                                    id = "court-configured-id",
                                    name = "Court A",
                                    status = "ACTIVE",
                                    availabilityStatus = CourtAvailabilitySetupStatus.CONFIGURED,
                                ),
                                MyComplexHubCourt(
                                    id = "court-pending-id",
                                    name = "Court B",
                                    status = "ACTIVE",
                                    availabilityStatus = CourtAvailabilitySetupStatus.PENDING,
                                ),
                            ),
                    )
                )
        ),
    private val failure: Throwable? = null,
    private val failures: ArrayDeque<Throwable> = ArrayDeque(),
) : IComplexRepository {
  var hubRequests = 0

  override suspend fun getMyComplexHub(): MyComplexHub {
    hubRequests += 1
    failures.removeFirstOrNull()?.let { throw it }
    failure?.let { throw it }
    return hub
  }

  override suspend fun getProvinces() = error("Unused in this test")

  override suspend fun getCantons(provinceId: String) = error("Unused in this test")

  override suspend fun getServices(
      scope: io.github.themonstersp4.mejengueros.domain.model.ServiceScope
  ) = error("Unused in this test")

  override suspend fun createComplex(
      request: io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
  ) = error("Unused in this test")
}
