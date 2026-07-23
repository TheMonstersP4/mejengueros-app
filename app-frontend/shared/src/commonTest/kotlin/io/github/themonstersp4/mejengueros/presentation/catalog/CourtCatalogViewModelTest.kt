package io.github.themonstersp4.mejengueros.presentation.catalog

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogPage
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CourtCatalogViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Test
  fun initLoadsCatalogAndBuildsFilterOptions() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = RecordingCourtCatalogRepository()
          val viewModel = CourtCatalogViewModel(repository, this)

          advanceUntilIdle()

          assertEquals(listOf(CatalogRequest("", null, null)), repository.requests)
          assertEquals(
              listOf("court-1", "court-2", "court-3", "court-5"),
              viewModel.uiState.value.visibleCourts.map { it.id },
          )
          assertEquals(
              listOf(
                  CatalogFilterOption(id = "province-2", label = "Alajuela"),
                  CatalogFilterOption(id = "province-3", label = "Heredia"),
                  CatalogFilterOption(id = "province-1", label = "San José"),
              ),
              viewModel.uiState.value.availableProvinces,
          )
          assertEquals(
              listOf(
                  CatalogFilterOption(id = "canton-1", label = "Escazú"),
                  CatalogFilterOption(id = "canton-5", label = "Grecia"),
                  CatalogFilterOption(id = "canton-2", label = "Moravia"),
                  CatalogFilterOption(id = "canton-3", label = "Moravia"),
              ),
              viewModel.uiState.value.availableCantons,
          )
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun searchQueryDebouncesBurstAndRefetchesCatalogUsingLatestRemoteQueryParameters() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = RecordingCourtCatalogRepository()
          val viewModel = CourtCatalogViewModel(repository, this)
          advanceUntilIdle()

          viewModel.updateSearchQuery("m")
          viewModel.updateSearchQuery("mo")
          viewModel.updateSearchQuery("mor")
          viewModel.updateSearchQuery("moravia")

          assertEquals("moravia", viewModel.uiState.value.searchQuery)
          assertEquals(1, repository.requests.size)

          advanceTimeBy(299)
          assertEquals(1, repository.requests.size)

          advanceTimeBy(1)
          advanceUntilIdle()
          assertEquals(
              listOf(
                  CatalogRequest("", null, null),
                  CatalogRequest(searchQuery = "moravia", provinceId = null, cantonId = null),
              ),
              repository.requests,
          )
          assertEquals(
              listOf("court-2", "court-3"),
              viewModel.uiState.value.visibleCourts.map { it.id },
          )

          viewModel.updateSearchQuery("mejengas")
          advanceTimeBy(300)
          advanceUntilIdle()
          assertEquals(
              CatalogRequest(searchQuery = "mejengas", provinceId = null, cantonId = null),
              repository.requests.last(),
          )
          assertEquals(listOf("court-1"), viewModel.uiState.value.visibleCourts.map { it.id })
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun selectingProvinceAndCantonUsesIdsForRemoteFiltering() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = RecordingCourtCatalogRepository()
          val viewModel = CourtCatalogViewModel(repository, this)
          advanceUntilIdle()

          viewModel.selectProvince("province-1")
          advanceUntilIdle()
          assertEquals(
              CatalogRequest(searchQuery = "", provinceId = "province-1", cantonId = null),
              repository.requests.last(),
          )
          assertEquals(
              listOf("court-1", "court-2"),
              viewModel.uiState.value.visibleCourts.map { it.id },
          )
          assertEquals(
              listOf(
                  CatalogFilterOption(id = "canton-1", label = "Escazú"),
                  CatalogFilterOption(id = "canton-2", label = "Moravia"),
              ),
              viewModel.uiState.value.availableCantons,
          )

          viewModel.selectCanton("canton-1")
          advanceUntilIdle()
          assertEquals(
              CatalogRequest(searchQuery = "", provinceId = "province-1", cantonId = "canton-1"),
              repository.requests.last(),
          )
          assertEquals(listOf("court-1"), viewModel.uiState.value.visibleCourts.map { it.id })

          viewModel.selectProvince("province-2")
          advanceUntilIdle()
          assertEquals(4, repository.requests.size)
          assertEquals(
              CatalogRequest(searchQuery = "", provinceId = "province-2", cantonId = null),
              repository.requests.last(),
          )
          assertNull(viewModel.uiState.value.selectedCantonId)
          assertEquals(listOf("court-5"), viewModel.uiState.value.visibleCourts.map { it.id })
          assertEquals(
              listOf(CatalogFilterOption(id = "canton-5", label = "Grecia")),
              viewModel.uiState.value.availableCantons,
          )

          viewModel.selectCanton(null)
          advanceUntilIdle()
          assertEquals(4, repository.requests.size)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun successfulEmptyFilteredResponsePreservesSelectedProvinceAndCanton() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository =
              RecordingCourtCatalogRepository(
                  responses =
                      mutableListOf(
                          allCatalogCourts,
                          listOf(allCatalogCourts.first()),
                          emptyList(),
                      )
              )
          val viewModel = CourtCatalogViewModel(repository, this)
          advanceUntilIdle()

          viewModel.selectProvince("province-1")
          advanceUntilIdle()
          viewModel.selectCanton("canton-1")
          advanceUntilIdle()

          assertEquals(
              CatalogRequest(searchQuery = "", provinceId = "province-1", cantonId = "canton-1"),
              repository.requests.last(),
          )
          assertEquals("province-1", viewModel.uiState.value.selectedProvinceId)
          assertEquals("San José", viewModel.uiState.value.selectedProvince?.label)
          assertEquals("canton-1", viewModel.uiState.value.selectedCantonId)
          assertEquals("Escazú", viewModel.uiState.value.selectedCanton?.label)
          assertTrue(viewModel.uiState.value.visibleCourts.isEmpty())
          assertNull(viewModel.uiState.value.loadErrorMessage)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun initFailureExposesLoadErrorAndKeepsCatalogEmpty() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val viewModel = CourtCatalogViewModel(FailingCourtCatalogRepository(), this)

          advanceUntilIdle()

          assertFalse(viewModel.uiState.value.isLoading)
          assertTrue(viewModel.uiState.value.visibleCourts.isEmpty())
          assertNotNull(viewModel.uiState.value.loadErrorMessage)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun retryLoadClearsErrorAndRestoresCatalogAfterTransientFailure() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = FlakyCourtCatalogRepository()
          val viewModel = CourtCatalogViewModel(repository, this)

          advanceUntilIdle()
          assertNotNull(viewModel.uiState.value.loadErrorMessage)

          viewModel.retryLoad()
          advanceUntilIdle()

          assertNull(viewModel.uiState.value.loadErrorMessage)
          assertEquals(
              listOf("court-1", "court-2", "court-3", "court-5"),
              viewModel.uiState.value.visibleCourts.map { it.id },
          )
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun firstPageDoesNotLoadTheWholeCatalogAndExposesNextPage() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = PagedCourtCatalogRepository(manyCatalogCourts)
          val viewModel = CourtCatalogViewModel(repository, this)

          advanceUntilIdle()

          // The first load stops at one page instead of the whole catalog.
          assertEquals(
              CourtCatalogPage.DEFAULT_PAGE_SIZE,
              viewModel.uiState.value.visibleCourts.size,
          )
          assertTrue(viewModel.uiState.value.hasNextPage)
          assertEquals(1, viewModel.uiState.value.currentPage)
          // The total is exposed even though only the first page is loaded, so
          // the UI can show "showing X of TOTAL".
          assertEquals(MANY_CATALOG_COURT_COUNT, viewModel.uiState.value.totalCourts)
          assertEquals(
              listOf(
                  CatalogRequest(
                      "",
                      null,
                      null,
                      page = 1,
                      pageSize = CourtCatalogPage.DEFAULT_PAGE_SIZE,
                  )
              ),
              repository.requests,
          )
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun loadNextPageAppendsFollowingPageWithoutDuplicates() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = PagedCourtCatalogRepository(manyCatalogCourts)
          val viewModel = CourtCatalogViewModel(repository, this)
          advanceUntilIdle()

          viewModel.loadNextPage()
          advanceUntilIdle()

          val visible = viewModel.uiState.value.visibleCourts
          assertEquals(MANY_CATALOG_COURT_COUNT, visible.size)
          assertEquals(visible.size, visible.map { it.id }.toSet().size)
          assertFalse(viewModel.uiState.value.hasNextPage)
          assertEquals(2, viewModel.uiState.value.currentPage)
          assertEquals(2, repository.requests.last().page)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun loadNextPageIgnoresRepeatedTriggersWhileAPageIsAlreadyLoading() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = PagedCourtCatalogRepository(manyCatalogCourts)
          val viewModel = CourtCatalogViewModel(repository, this)
          advanceUntilIdle()

          viewModel.loadNextPage()
          viewModel.loadNextPage()
          viewModel.loadNextPage()
          advanceUntilIdle()

          // Only the first-page load plus a single next-page load happened.
          assertEquals(2, repository.requests.size)
          assertEquals(MANY_CATALOG_COURT_COUNT, viewModel.uiState.value.visibleCourts.size)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun nextPageFailureKeepsLoadedCourtsAndRetrySucceeds() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = NextPageFlakyCourtCatalogRepository(manyCatalogCourts)
          val viewModel = CourtCatalogViewModel(repository, this)
          advanceUntilIdle()

          viewModel.loadNextPage()
          advanceUntilIdle()

          // The second page failed but the first page is untouched.
          assertNotNull(viewModel.uiState.value.nextPageErrorMessage)
          assertEquals(
              CourtCatalogPage.DEFAULT_PAGE_SIZE,
              viewModel.uiState.value.visibleCourts.size,
          )
          assertFalse(viewModel.uiState.value.canLoadNextPage)

          viewModel.retryNextPage()
          advanceUntilIdle()

          assertNull(viewModel.uiState.value.nextPageErrorMessage)
          assertEquals(MANY_CATALOG_COURT_COUNT, viewModel.uiState.value.visibleCourts.size)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun initLoadsAvailableServicesFromCatalogSortedByLabel() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository =
              RecordingCourtCatalogRepository(
                  services =
                      listOf(
                          ServiceCatalogItem("service-2", "Parqueo", ServiceScope.COMPLEX),
                          ServiceCatalogItem("service-1", "Iluminación", ServiceScope.COURT),
                      )
              )
          val viewModel = CourtCatalogViewModel(repository, this)

          advanceUntilIdle()

          assertEquals(
              listOf(
                  CatalogFilterOption(id = "service-1", label = "Iluminación"),
                  CatalogFilterOption(id = "service-2", label = "Parqueo"),
              ),
              viewModel.uiState.value.availableServices,
          )
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun togglingServicesAccumulatesIdsAndClearingResetsThem() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository =
              RecordingCourtCatalogRepository(
                  services =
                      listOf(
                          ServiceCatalogItem("service-1", "Sintético", ServiceScope.COURT),
                          ServiceCatalogItem("service-2", "Parqueo", ServiceScope.COMPLEX),
                      )
              )
          val viewModel = CourtCatalogViewModel(repository, this)
          advanceUntilIdle()

          viewModel.toggleService("service-1")
          advanceUntilIdle()
          viewModel.toggleService("service-2")
          advanceUntilIdle()

          // Both selected services travel together so the backend can require all of them.
          assertEquals(setOf("service-1", "service-2"), viewModel.uiState.value.selectedServiceIds)
          assertEquals(
              listOf("service-1", "service-2"),
              repository.requests.last().serviceIds,
          )
          // selectedServices follows availableServices' label order, not toggle order.
          assertEquals(
              listOf("Parqueo", "Sintético"),
              viewModel.uiState.value.selectedServices.map { it.label },
          )

          // Toggling an active service removes it.
          viewModel.toggleService("service-1")
          advanceUntilIdle()
          assertEquals(setOf("service-2"), viewModel.uiState.value.selectedServiceIds)
          assertEquals(listOf("service-2"), repository.requests.last().serviceIds)

          viewModel.clearServices()
          advanceUntilIdle()
          assertTrue(viewModel.uiState.value.selectedServiceIds.isEmpty())
          assertTrue(repository.requests.last().serviceIds.isEmpty())
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun serviceFilterIsPreservedWhilePaginating() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = PagedCourtCatalogRepository(manyCatalogCourts)
          val viewModel = CourtCatalogViewModel(repository, this)
          advanceUntilIdle()

          viewModel.toggleService("service-1")
          advanceUntilIdle()
          viewModel.loadNextPage()
          advanceUntilIdle()

          val lastRequest = repository.requests.last()
          assertEquals(2, lastRequest.page)
          assertEquals(listOf("service-1"), lastRequest.serviceIds)
          assertEquals(setOf("service-1"), viewModel.uiState.value.selectedServiceIds)
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun changingSearchResetsPaginationBackToFirstPage() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = PagedCourtCatalogRepository(manyCatalogCourts)
          val viewModel = CourtCatalogViewModel(repository, this)
          advanceUntilIdle()
          viewModel.loadNextPage()
          advanceUntilIdle()
          assertEquals(MANY_CATALOG_COURT_COUNT, viewModel.uiState.value.visibleCourts.size)

          viewModel.updateSearchQuery("cancha 01")
          advanceTimeBy(300)
          advanceUntilIdle()

          // A new search restarts from page 1 and drops the accumulated pages.
          assertEquals(1, viewModel.uiState.value.currentPage)
          assertEquals(1, repository.requests.last().page)
          assertTrue(
              viewModel.uiState.value.visibleCourts.size <= CourtCatalogPage.DEFAULT_PAGE_SIZE,
          )
        } finally {
          Dispatchers.resetMain()
        }
      }
}

private fun List<CourtCatalogItem>.toCatalogPage(page: Int, pageSize: Int): CourtCatalogPage {
  val fromIndex = (page - 1) * pageSize
  val windowed =
      if (fromIndex >= size) emptyList()
      else subList(fromIndex, minOf(fromIndex + pageSize, size)).toList()
  val totalPages = if (isEmpty()) 0 else (size + pageSize - 1) / pageSize
  return CourtCatalogPage(
      items = windowed,
      page = page,
      pageSize = pageSize,
      totalItems = size,
      totalPages = totalPages,
  )
}

private class FakeCourtCatalogRepository : ICourtCatalogRepository {
  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
      serviceIds: List<String>,
      page: Int,
      pageSize: Int,
  ): CourtCatalogPage =
      allCatalogCourts
          .filter { searchQuery.isNullOrBlank() || it.matchesSearch(searchQuery) }
          .filter { provinceId == null || it.provinceId == provinceId }
          .filter { cantonId == null || it.cantonId == cantonId }
          .toCatalogPage(page, pageSize)
}

private class RecordingCourtCatalogRepository(
    private val responses: MutableList<List<CourtCatalogItem>> = mutableListOf(),
    private val services: List<ServiceCatalogItem> = emptyList(),
) : ICourtCatalogRepository {
  val requests = mutableListOf<CatalogRequest>()

  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
      serviceIds: List<String>,
      page: Int,
      pageSize: Int,
  ): CourtCatalogPage {
    requests += CatalogRequest(searchQuery, provinceId, cantonId, serviceIds, page, pageSize)
    if (responses.isNotEmpty()) {
      return responses.removeFirst().toCatalogPage(page, pageSize)
    }

    return FakeCourtCatalogRepository()
        .getCatalogCourts(searchQuery, provinceId, cantonId, serviceIds, page, pageSize)
  }

  override suspend fun getServiceCatalog(): List<ServiceCatalogItem> = services
}

private data class CatalogRequest(
    val searchQuery: String?,
    val provinceId: String?,
    val cantonId: String?,
    val serviceIds: List<String> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = CourtCatalogPage.DEFAULT_PAGE_SIZE,
)

private val allCatalogCourts =
    listOf(
        CourtCatalogItem(
            id = "court-1",
            complexId = "complex-1",
            complexName = "Mejengas CR",
            courtName = "Cancha 1",
            provinceId = "province-1",
            provinceName = "San José",
            cantonId = "canton-1",
            cantonName = "Escazú",
            services = listOf("Sintetico", "Iluminacion"),
            ratingAverage = 4.5,
            ratingCount = 2,
            imageUrl = null,
            isReservableToday = true,
        ),
        CourtCatalogItem(
            id = "court-2",
            complexId = "complex-2",
            complexName = "Moravia FC",
            courtName = "Cancha A",
            provinceId = "province-1",
            provinceName = "San José",
            cantonId = "canton-2",
            cantonName = "Moravia",
            services = listOf("Sintetico"),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            isReservableToday = true,
        ),
        CourtCatalogItem(
            id = "court-3",
            complexId = "complex-3",
            complexName = "Moravia Norte",
            courtName = "Cancha B",
            provinceId = "province-3",
            provinceName = "Heredia",
            cantonId = "canton-3",
            cantonName = "Moravia",
            services = listOf("Natural"),
            ratingAverage = 4.0,
            ratingCount = 4,
            imageUrl = null,
            isReservableToday = true,
        ),
        CourtCatalogItem(
            id = "court-5",
            complexId = "complex-5",
            complexName = "Grecia Arena",
            courtName = "Cancha Central",
            provinceId = "province-2",
            provinceName = "Alajuela",
            cantonId = "canton-5",
            cantonName = "Grecia",
            services = listOf("Hibrido", "Parqueo"),
            ratingAverage = 3.0,
            ratingCount = 1,
            imageUrl = null,
            isReservableToday = false,
        ),
    )

private fun CourtCatalogItem.matchesSearch(searchQuery: String): Boolean {
  val normalizedQuery = searchQuery.trim().lowercase()
  return complexName.lowercase().contains(normalizedQuery) ||
      courtName.lowercase().contains(normalizedQuery)
}

private class FailingCourtCatalogRepository : ICourtCatalogRepository {
  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
      serviceIds: List<String>,
      page: Int,
      pageSize: Int,
  ): CourtCatalogPage {
    throw IllegalStateException("boom")
  }
}

private class FlakyCourtCatalogRepository : ICourtCatalogRepository {
  private var attempts = 0

  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
      serviceIds: List<String>,
      page: Int,
      pageSize: Int,
  ): CourtCatalogPage {
    attempts += 1
    if (attempts == 1) {
      throw IllegalStateException("temporary failure")
    }

    return FakeCourtCatalogRepository()
        .getCatalogCourts(null, null, null, emptyList(), page, pageSize)
  }
}

// One full page plus a partial second page, expressed relative to the page
// size so the pagination tests stay valid if the default page size changes.
private const val MANY_CATALOG_COURT_COUNT = CourtCatalogPage.DEFAULT_PAGE_SIZE + 5

private val manyCatalogCourts: List<CourtCatalogItem> =
    (0 until MANY_CATALOG_COURT_COUNT).map { index ->
      val label = index.toString().padStart(2, '0')
      CourtCatalogItem(
          id = "court-$label",
          complexId = "complex-$label",
          complexName = "Complejo $label",
          courtName = "Cancha $label",
          provinceId = "province-1",
          provinceName = "San José",
          cantonId = "canton-1",
          cantonName = "Escazú",
          services = listOf("Sintetico"),
          ratingAverage = if (index % 3 == 0) null else (1 + index % 5).toDouble(),
          ratingCount = index % 7,
          imageUrl = null,
          isReservableToday = index % 2 == 0,
      )
    }

private class PagedCourtCatalogRepository(
    private val courts: List<CourtCatalogItem>,
) : ICourtCatalogRepository {
  val requests = mutableListOf<CatalogRequest>()

  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
      serviceIds: List<String>,
      page: Int,
      pageSize: Int,
  ): CourtCatalogPage {
    requests += CatalogRequest(searchQuery, provinceId, cantonId, serviceIds, page, pageSize)
    return courts
        .filter { searchQuery.isNullOrBlank() || it.matchesSearch(searchQuery) }
        .filter { provinceId == null || it.provinceId == provinceId }
        .filter { cantonId == null || it.cantonId == cantonId }
        .toCatalogPage(page, pageSize)
  }
}

private class NextPageFlakyCourtCatalogRepository(
    private val courts: List<CourtCatalogItem>,
) : ICourtCatalogRepository {
  private var nextPageAttempts = 0

  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
      serviceIds: List<String>,
      page: Int,
      pageSize: Int,
  ): CourtCatalogPage {
    if (page > 1) {
      nextPageAttempts += 1
      if (nextPageAttempts == 1) {
        throw IllegalStateException("temporary next page failure")
      }
    }

    return courts.toCatalogPage(page, pageSize)
  }
}
