package io.github.themonstersp4.mejengueros.presentation.complexes

import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateCourtRequest
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.CreatedCourt
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHub
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import io.github.themonstersp4.mejengueros.monitoring.ErrorReporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AddCourtViewModelTest {
  @Test
  fun initLoadsCourtServices() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

    val viewModel =
        AddCourtViewModel(
            complexId = "complex-id",
            complexName = "North Sports Center",
            repository = repository,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    assertEquals(listOf(ServiceScope.COURT), repository.serviceRequests)
    assertEquals(1, viewModel.uiState.value.courtServices.size)
    assertNull(viewModel.uiState.value.errorMessage)
  }

  @Test
  fun submitCreatesCourtAndExposesCreatedCourtUntilAcknowledged() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AddCourtViewModel(
            complexId = "complex-id",
            complexName = "North Sports Center",
            repository = repository,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    viewModel.updateCourtName("Court B")
    viewModel.toggleCourtService("court-service-id")
    viewModel.submit()
    advanceUntilIdle()

    assertEquals(
        CreateCourtRequest(name = "Court B", serviceIds = listOf("court-service-id")),
        repository.addCourtRequest,
    )
    assertEquals("complex-id", repository.addCourtComplexId)
    assertEquals("court-id", viewModel.uiState.value.createdCourt?.id)

    viewModel.acknowledgeSuccess()

    assertNull(viewModel.uiState.value.createdCourt)
  }

  @Test
  fun submitRequiresCourtNameAndAtLeastOneService() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AddCourtViewModel(
            complexId = "complex-id",
            complexName = "North Sports Center",
            repository = repository,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    viewModel.submit()
    advanceUntilIdle()

    assertEquals(
        "Completá el nombre de la cancha y elegí al menos un servicio de cancha.",
        viewModel.uiState.value.errorMessage,
    )
    assertNull(repository.addCourtRequest)
  }

  @Test
  fun submitMapsNotFoundApiFailuresToControlledCopy() = runTest {
    val repository =
        FakeComplexRepository(
            addCourtFailures = ArrayDeque(listOf(AppApiException(404, "Complex not found")))
        )
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AddCourtViewModel(
            complexId = "complex-id",
            complexName = "North Sports Center",
            repository = repository,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    viewModel.updateCourtName("Court B")
    viewModel.toggleCourtService("court-service-id")
    viewModel.submit()
    advanceUntilIdle()

    assertEquals("No encontramos el complejo seleccionado.", viewModel.uiState.value.errorMessage)
  }

  @Test
  fun refreshServicesReportsRecoverableFailureAndRetryStillLoadsCatalog() = runTest {
    val errorReporter = FakeErrorReporter()
    val repository =
        FakeComplexRepository(
            serviceFailures =
                ArrayDeque(
                    listOf(AppApiException(statusCode = 503, message = "Service catalog down"))
                )
        )
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))

    val viewModel =
        AddCourtViewModel(
            complexId = "complex-id",
            complexName = "North Sports Center",
            repository = repository,
            errorReporter = errorReporter,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    assertEquals(
        "No pudimos cargar los servicios de cancha. Intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )
    assertEquals(1, errorReporter.events.size)
    assertEquals("add_court_services_load_failed", errorReporter.events.single().name)
    assertEquals(
        mapOf(
            "operation" to "load_services",
            "error_source" to "app_api",
            "status_code" to "503",
        ),
        errorReporter.events.single().attributes,
    )

    viewModel.refreshServices()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.errorMessage)
    assertEquals(1, viewModel.uiState.value.courtServices.size)
    assertEquals(listOf(ServiceScope.COURT, ServiceScope.COURT), repository.serviceRequests)
  }

  @Test
  fun submitReportsRecoverableFailureAndRetryStillSucceeds() = runTest {
    val errorReporter = FakeErrorReporter()
    val repository =
        FakeComplexRepository(
            addCourtFailures =
                ArrayDeque(listOf(AppApiException(statusCode = 500, message = "Backend exploded")))
        )
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
        AddCourtViewModel(
            complexId = "complex-id",
            complexName = "North Sports Center",
            repository = repository,
            errorReporter = errorReporter,
            coroutineScope = scope,
        )
    advanceUntilIdle()

    viewModel.updateCourtName("Court B")
    viewModel.toggleCourtService("court-service-id")
    viewModel.submit()
    advanceUntilIdle()

    assertEquals(
        "No pudimos guardar la cancha. Intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )
    assertEquals(1, errorReporter.events.size)
    assertEquals("add_court_submit_failed", errorReporter.events.single().name)
    assertEquals(
        mapOf(
            "operation" to "submit",
            "error_source" to "app_api",
            "status_code" to "500",
        ),
        errorReporter.events.single().attributes,
    )

    viewModel.submit()
    advanceUntilIdle()

    assertEquals(2, repository.addCourtAttempts)
    assertEquals("court-id", viewModel.uiState.value.createdCourt?.id)
    assertNull(viewModel.uiState.value.formErrorMessage)
  }

  private class FakeComplexRepository(
      private val serviceFailures: ArrayDeque<Throwable> = ArrayDeque(),
      private val addCourtFailures: ArrayDeque<Throwable> = ArrayDeque(),
  ) : IComplexRepository {
    val serviceRequests = mutableListOf<ServiceScope>()
    var addCourtComplexId: String? = null
    var addCourtRequest: CreateCourtRequest? = null
    var addCourtAttempts = 0

    override suspend fun getProvinces(): List<Province> = emptyList()

    override suspend fun getCantons(provinceId: String): List<Canton> = emptyList()

    override suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem> {
      serviceRequests += scope
      serviceFailures.removeFirstOrNull()?.let { throw it }
      return listOf(ServiceCatalogItem(id = "court-service-id", name = "Lighting", scope = scope))
    }

    override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex =
        error("Unused in this test")

    override suspend fun addCourt(complexId: String, request: CreateCourtRequest): CreatedCourt {
      addCourtComplexId = complexId
      addCourtRequest = request
      addCourtAttempts += 1
      addCourtFailures.removeFirstOrNull()?.let { throw it }
      return CreatedCourt(id = "court-id", complexId = complexId, name = request.name)
    }

    override suspend fun getMyComplexHub(): MyComplexHub = error("Unused in this test")
  }

  private class FakeErrorReporter : ErrorReporter {
    val events = mutableListOf<ReportedFailure>()

    override fun reportRecoverableFailure(name: String, attributes: Map<String, String>) {
      events += ReportedFailure(name = name, attributes = attributes)
    }
  }

  private data class ReportedFailure(
      val name: String,
      val attributes: Map<String, String>,
  )
}
