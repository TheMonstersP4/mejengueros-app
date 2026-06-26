package io.github.themonstersp4.mejengueros.presentation.complexes

import io.github.themonstersp4.mejengueros.data.remote.AppApiException
import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexDetails
import io.github.themonstersp4.mejengueros.domain.model.CreateComplexRequest
import io.github.themonstersp4.mejengueros.domain.model.CreateFirstCourtDetails
import io.github.themonstersp4.mejengueros.domain.model.CreatedComplex
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.domain.repository.IComplexRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class CreateComplexViewModelTest {
  @Test
  fun submitCreatesComplexAndExposesSuccessStateUntilAcknowledged() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    viewModel.updateComplexName("North Sports Center")
    viewModel.selectProvince("province-1")
    advanceUntilIdle()
    viewModel.selectCanton("canton-1")
    viewModel.updateComplexAddress("123 Main Street")
    viewModel.updateSelectedLocation(latitude = 9.935, longitude = -84.091)
    viewModel.toggleComplexService("complex-service-id")
    viewModel.goToFirstCourtStep()
    viewModel.updateFirstCourtName("Court A")
    viewModel.toggleCourtService("court-service-id")

    viewModel.submit()
    advanceUntilIdle()

    assertEquals(
        CreateComplexRequest(
            complex =
                CreateComplexDetails(
                    name = "North Sports Center",
                    provinceId = "province-1",
                    cantonId = "canton-1",
                    address = "123 Main Street",
                    latitude = 9.935,
                    longitude = -84.091,
                    serviceIds = listOf("complex-service-id"),
                ),
            firstCourt =
                CreateFirstCourtDetails(
                    name = "Court A",
                    serviceIds = listOf("court-service-id"),
                ),
        ),
        repository.receivedRequest,
    )
    assertEquals(
        "Complejo y primera cancha creados correctamente.",
        viewModel.uiState.value.successMessage,
    )
    assertEquals(CreateComplexStep.Complex, viewModel.uiState.value.currentStep)
    assertEquals(1, viewModel.uiState.value.provinces.size)
    assertEquals(1, viewModel.uiState.value.courtServices.size)
    assertNull(viewModel.uiState.value.selectedProvinceId)
    assertEquals("complex-id", viewModel.uiState.value.createdComplex?.complexId)

    viewModel.acknowledgeSuccess()

    assertNull(viewModel.uiState.value.successMessage)
    assertNull(viewModel.uiState.value.createdComplex)
    scope.cancel()
  }

  @Test
  fun selectProvinceClearsSelectedCantonAndLoadsCantonsForNewProvince() = runTest {
    val repository =
        FakeComplexRepository(
            provinces =
                listOf(
                    Province(id = "province-1", code = "SJ", name = "San José"),
                    Province(id = "province-2", code = "AL", name = "Alajuela"),
                ),
            cantonsByProvince =
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
                ),
        )
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    viewModel.selectProvince("province-1")
    advanceUntilIdle()
    viewModel.selectCanton("canton-1")

    viewModel.selectProvince("province-2")

    assertNull(viewModel.uiState.value.selectedCantonId)
    advanceUntilIdle()
    assertEquals("province-2", viewModel.uiState.value.selectedProvinceId)
    assertEquals(listOf("province-1", "province-2"), repository.cantonRequests)
    assertEquals("canton-2", viewModel.uiState.value.cantons.single().id)
    scope.cancel()
  }

  @Test
  fun submitRequiresAtLeastOneCourtServiceSelection() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    viewModel.updateComplexName("North Sports Center")
    viewModel.selectProvince("province-1")
    advanceUntilIdle()
    viewModel.selectCanton("canton-1")
    viewModel.updateComplexAddress("123 Main Street")
    viewModel.goToFirstCourtStep()
    viewModel.updateFirstCourtName("Court A")

    viewModel.submit()
    advanceUntilIdle()

    assertNull(repository.receivedRequest)
    assertEquals(
        "Completá el nombre de la primera cancha y elegí al menos un servicio de cancha.",
        viewModel.uiState.value.errorMessage,
    )
    scope.cancel()
  }

  @Test
  fun selectProvinceIgnoresStaleCantonsFromAnOlderProvinceRequest() = runTest {
    val repository =
        FakeComplexRepository(
            provinces =
                listOf(
                    Province(id = "province-1", code = "SJ", name = "San José"),
                    Province(id = "province-2", code = "AL", name = "Alajuela"),
                )
        )
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    val province1Deferred = CompletableDeferred<List<Canton>>()
    val province2Deferred = CompletableDeferred<List<Canton>>()
    repository.cantonResponsesByProvince["province-1"] = province1Deferred
    repository.cantonResponsesByProvince["province-2"] = province2Deferred

    viewModel.selectProvince("province-1")
    runCurrent()

    assertEquals(listOf("province-1"), repository.cantonRequests)

    viewModel.selectProvince("province-2")
    runCurrent()

    assertEquals(listOf("province-1", "province-2"), repository.cantonRequests)

    province2Deferred.complete(
        listOf(Canton(id = "canton-2", provinceId = "province-2", code = "AL-GRE", name = "Grecia"))
    )
    advanceUntilIdle()

    assertEquals("province-2", viewModel.uiState.value.selectedProvinceId)
    assertEquals(listOf("canton-2"), viewModel.uiState.value.cantons.map { it.id })
    assertFalse(viewModel.uiState.value.isLoadingCantons)

    province1Deferred.complete(
        listOf(Canton(id = "canton-1", provinceId = "province-1", code = "SJ-ESC", name = "Escazú"))
    )
    advanceUntilIdle()

    assertEquals("province-2", viewModel.uiState.value.selectedProvinceId)
    assertEquals(listOf("canton-2"), viewModel.uiState.value.cantons.map { it.id })
    scope.cancel()
  }

  @Test
  fun retrySelectedProvinceCantonsReloadsSameProvinceAfterFailure() = runTest {
    val repository =
        FakeComplexRepository(
            cantonFailuresByProvince = mutableMapOf("province-1" to 1),
        )
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    viewModel.selectProvince("province-1")
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.hasCantonLoadFailure)
    assertEquals(
        "No pudimos cargar los cantones de la provincia elegida. Intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )
    assertEquals(listOf("province-1"), repository.cantonRequests)

    viewModel.retrySelectedProvinceCantons()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.hasCantonLoadFailure)
    assertFalse(viewModel.uiState.value.isLoadingCantons)
    assertNull(viewModel.uiState.value.errorMessage)
    assertEquals(listOf("province-1", "province-1"), repository.cantonRequests)
    assertEquals(listOf("canton-1"), viewModel.uiState.value.cantons.map { it.id })
    scope.cancel()
  }

  @Test
  fun cantonRetryStateRemainsVisibleAfterUnrelatedFormEdit() = runTest {
    val repository =
        FakeComplexRepository(
            cantonFailuresByProvince = mutableMapOf("province-1" to 1),
        )
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    viewModel.selectProvince("province-1")
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.hasCantonLoadFailure)
    assertEquals(
        "No pudimos cargar los cantones de la provincia elegida. Intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )

    viewModel.updateComplexName("North Sports Center")

    assertTrue(viewModel.uiState.value.hasCantonLoadFailure)
    assertEquals(
        "No pudimos cargar los cantones de la provincia elegida. Intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )
    assertEquals(
        "No pudimos cargar los cantones de la provincia elegida. Intentá de nuevo.",
        viewModel.uiState.value.loadErrorMessage,
    )
    scope.cancel()
  }

  @Test
  fun refreshCatalogsIgnoresCancelledBootstrapResults() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val firstBootstrap = CompletableDeferred<List<Province>>()
    val secondBootstrap = CompletableDeferred<List<Province>>()
    repository.provinceResponses += firstBootstrap
    repository.provinceResponses += secondBootstrap
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    runCurrent()

    assertEquals(1, repository.provinceRequests)

    viewModel.refreshCatalogs()
    runCurrent()

    assertEquals(2, repository.provinceRequests)

    secondBootstrap.complete(listOf(Province(id = "province-2", code = "AL", name = "Alajuela")))
    advanceUntilIdle()

    assertEquals(listOf("province-2"), viewModel.uiState.value.provinces.map { it.id })

    firstBootstrap.complete(listOf(Province(id = "province-1", code = "SJ", name = "San José")))
    advanceUntilIdle()

    assertEquals(listOf("province-2"), viewModel.uiState.value.provinces.map { it.id })
    scope.cancel()
  }

  @Test
  fun refreshCatalogsRetriesBootstrapAfterFailure() = runTest {
    val repository =
        FakeComplexRepository(
            provincesFailureCount = 1,
            provinces = listOf(Province(id = "province-1", code = "SJ", name = "San José")),
        )
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    assertEquals(
        "No pudimos cargar los catálogos del complejo. Intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )
    assertTrue(viewModel.uiState.value.hasCatalogLoadFailure)

    viewModel.refreshCatalogs()
    advanceUntilIdle()

    assertEquals(2, repository.provinceRequests)
    assertFalse(viewModel.uiState.value.isLoadingCatalogs)
    assertNull(viewModel.uiState.value.errorMessage)
    assertFalse(viewModel.uiState.value.hasCatalogLoadFailure)
    assertEquals(listOf("province-1"), viewModel.uiState.value.provinces.map { it.id })
    assertEquals(
        listOf("complex-service-id"),
        viewModel.uiState.value.complexServices.map { it.id },
    )
    assertEquals(listOf("court-service-id"), viewModel.uiState.value.courtServices.map { it.id })
    scope.cancel()
  }

  @Test
  fun refreshCatalogsClearsSelectionsThatNoLongerExist() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    viewModel.selectProvince("province-1")
    advanceUntilIdle()
    viewModel.selectCanton("canton-1")
    viewModel.toggleComplexService("complex-service-id")
    viewModel.goToFirstCourtStep()
    viewModel.updateFirstCourtName("Court A")
    viewModel.toggleCourtService("court-service-id")

    repository.provinces = listOf(Province(id = "province-2", code = "AL", name = "Alajuela"))
    repository.complexServices = emptyList()
    repository.courtServices = emptyList()

    viewModel.refreshCatalogs()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.selectedProvinceId)
    assertNull(viewModel.uiState.value.selectedCantonId)
    assertTrue(viewModel.uiState.value.cantons.isEmpty())
    assertTrue(viewModel.uiState.value.selectedComplexServiceIds.isEmpty())
    assertTrue(viewModel.uiState.value.selectedCourtServiceIds.isEmpty())
    assertFalse(viewModel.uiState.value.canSubmit)
    scope.cancel()
  }

  @Test
  fun refreshCatalogsClearsSelectedCantonWhenItNoLongerExistsInLoadedCantons() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    viewModel.selectProvince("province-1")
    advanceUntilIdle()
    viewModel.selectCanton("canton-1")

    repository.cantonsByProvince =
        mapOf(
            "province-1" to
                listOf(
                    Canton(
                        id = "canton-2",
                        provinceId = "province-1",
                        code = "SJ-DES",
                        name = "Desamparados",
                    )
                )
        )
    viewModel.refreshCatalogs()
    advanceUntilIdle()

    assertEquals("province-1", viewModel.uiState.value.selectedProvinceId)
    assertNull(viewModel.uiState.value.selectedCantonId)
    scope.cancel()
  }

  @Test
  fun refreshCatalogsClearsSelectedCantonAndBlocksProgressWhenReloadFails() = runTest {
    val repository = FakeComplexRepository()
    val scope = TestScope(StandardTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    viewModel.updateComplexName("North Sports Center")
    viewModel.selectProvince("province-1")
    advanceUntilIdle()
    viewModel.selectCanton("canton-1")
    viewModel.updateComplexAddress("123 Main Street")

    repository.cantonFailuresByProvince["province-1"] = 1

    viewModel.refreshCatalogs()
    advanceUntilIdle()

    assertEquals("province-1", viewModel.uiState.value.selectedProvinceId)
    assertNull(viewModel.uiState.value.selectedCantonId)
    assertTrue(viewModel.uiState.value.cantons.isEmpty())
    assertTrue(viewModel.uiState.value.hasCantonLoadFailure)
    assertFalse(viewModel.uiState.value.canGoToCourtStep)
    assertFalse(viewModel.uiState.value.canSubmit)

    viewModel.goToFirstCourtStep()

    assertEquals(CreateComplexStep.Complex, viewModel.uiState.value.currentStep)
    assertEquals(
        "No pudimos cargar los cantones de la provincia elegida. Intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )
    assertEquals(listOf("province-1", "province-1"), repository.cantonRequests)
    scope.cancel()
  }

  @Test
  fun submitFailureMapsBackendMessageToControlledCopy() = runTest {
    val repository =
        FakeComplexRepository(
            createFailure = AppApiException(statusCode = 500, message = "Backend exploded"),
        )
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    viewModel.updateComplexName("North Sports Center")
    viewModel.selectProvince("province-1")
    advanceUntilIdle()
    viewModel.selectCanton("canton-1")
    viewModel.updateComplexAddress("123 Main Street")
    viewModel.goToFirstCourtStep()
    viewModel.updateFirstCourtName("Court A")
    viewModel.toggleCourtService("court-service-id")

    viewModel.submit()
    advanceUntilIdle()

    assertEquals(
        "No pudimos crear el complejo en este momento. Intentá de nuevo.",
        viewModel.uiState.value.errorMessage,
    )
    assertFalse(viewModel.uiState.value.isSubmitting)
    scope.cancel()
  }

  @Test
  fun submitCancellationDoesNotExposeErrorMessage() = runTest {
    val repository = FakeComplexRepository(createFailure = CancellationException("cancelled"))
    val scope = TestScope(UnconfinedTestDispatcher(testScheduler))
    val viewModel = CreateComplexViewModel(repository, coroutineScope = scope)
    advanceUntilIdle()

    viewModel.updateComplexName("North Sports Center")
    viewModel.selectProvince("province-1")
    advanceUntilIdle()
    viewModel.selectCanton("canton-1")
    viewModel.updateComplexAddress("123 Main Street")
    viewModel.goToFirstCourtStep()
    viewModel.updateFirstCourtName("Court A")
    viewModel.toggleCourtService("court-service-id")

    viewModel.submit()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.errorMessage)
    assertFalse(viewModel.uiState.value.isSubmitting)
    scope.cancel()
  }

  private class FakeComplexRepository(
      var provinces: List<Province> =
          listOf(Province(id = "province-1", code = "SJ", name = "San José")),
      var cantonsByProvince: Map<String, List<Canton>> =
          mapOf(
              "province-1" to
                  listOf(
                      Canton(
                          id = "canton-1",
                          provinceId = "province-1",
                          code = "SJ-ESC",
                          name = "Escazú",
                      )
                  )
          ),
      var complexServices: List<ServiceCatalogItem> =
          listOf(
              ServiceCatalogItem(
                  id = "complex-service-id",
                  name = "Parking",
                  scope = ServiceScope.COMPLEX,
              )
          ),
      var courtServices: List<ServiceCatalogItem> =
          listOf(
              ServiceCatalogItem(
                  id = "court-service-id",
                  name = "Synthetic Grass",
                  scope = ServiceScope.COURT,
              )
          ),
      private val provincesFailureCount: Int = 0,
      val cantonFailuresByProvince: MutableMap<String, Int> = mutableMapOf(),
      private val createFailure: Throwable? = null,
  ) : IComplexRepository {
    var receivedRequest: CreateComplexRequest? = null
    val cantonRequests = mutableListOf<String>()
    val cantonResponsesByProvince = mutableMapOf<String, CompletableDeferred<List<Canton>>>()
    val provinceResponses = mutableListOf<CompletableDeferred<List<Province>>>()
    var provinceRequests = 0
    private var remainingProvinceFailures = provincesFailureCount

    override suspend fun getProvinces(): List<Province> {
      provinceRequests += 1
      provinceResponses.removeFirstOrNull()?.let {
        return it.await()
      }
      if (remainingProvinceFailures > 0) {
        remainingProvinceFailures -= 1
        throw IllegalStateException("Catalogs unavailable")
      }

      return provinces
    }

    override suspend fun getCantons(provinceId: String): List<Canton> {
      cantonRequests += provinceId
      cantonResponsesByProvince.remove(provinceId)?.let {
        return it.await()
      }
      cantonFailuresByProvince[provinceId]
          ?.takeIf { it > 0 }
          ?.let { remainingFailures ->
            cantonFailuresByProvince[provinceId] = remainingFailures - 1
            throw IllegalStateException("Cantons unavailable")
          }
      return cantonsByProvince[provinceId].orEmpty()
    }

    override suspend fun getServices(scope: ServiceScope): List<ServiceCatalogItem> =
        when (scope) {
          ServiceScope.COMPLEX -> complexServices
          ServiceScope.COURT -> courtServices
        }

    override suspend fun createComplex(request: CreateComplexRequest): CreatedComplex {
      receivedRequest = request
      createFailure?.let { throw it }
      return CreatedComplex(
          complexId = "complex-id",
          complexName = request.complex.name,
          complexAddress = request.complex.address,
          firstCourtId = "court-id",
          firstCourtName = request.firstCourt.name,
      )
    }
  }
}
