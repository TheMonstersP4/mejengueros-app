package io.github.themonstersp4.mejengueros.presentation.catalog

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
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
  fun searchQueryRefetchesCatalogUsingRemoteQueryParameters() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val repository = RecordingCourtCatalogRepository()
          val viewModel = CourtCatalogViewModel(repository, this)
          advanceUntilIdle()

          viewModel.updateSearchQuery("moravia")
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
}

private class FakeCourtCatalogRepository : ICourtCatalogRepository {
  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
  ): List<CourtCatalogItem> =
      allCatalogCourts
          .filter { searchQuery.isNullOrBlank() || it.matchesSearch(searchQuery) }
          .filter { provinceId == null || it.provinceId == provinceId }
          .filter { cantonId == null || it.cantonId == cantonId }
}

private class RecordingCourtCatalogRepository : ICourtCatalogRepository {
  val requests = mutableListOf<CatalogRequest>()

  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
  ): List<CourtCatalogItem> {
    requests += CatalogRequest(searchQuery, provinceId, cantonId)
    return FakeCourtCatalogRepository().getCatalogCourts(searchQuery, provinceId, cantonId)
  }
}

private data class CatalogRequest(
    val searchQuery: String?,
    val provinceId: String?,
    val cantonId: String?,
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
  ): List<CourtCatalogItem> {
    throw IllegalStateException("boom")
  }
}

private class FlakyCourtCatalogRepository : ICourtCatalogRepository {
  private var attempts = 0

  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
  ): List<CourtCatalogItem> {
    attempts += 1
    if (attempts == 1) {
      throw IllegalStateException("temporary failure")
    }

    return FakeCourtCatalogRepository().getCatalogCourts(null, null, null)
  }
}
