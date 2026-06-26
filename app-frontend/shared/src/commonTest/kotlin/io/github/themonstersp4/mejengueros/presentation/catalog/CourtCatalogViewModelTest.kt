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
          val viewModel = CourtCatalogViewModel(FakeCourtCatalogRepository(), this)

          advanceUntilIdle()

          assertEquals(
              listOf("court-1", "court-2", "court-5"),
              viewModel.uiState.value.visibleCourts.map { it.id },
          )
          assertEquals(listOf("Alajuela", "San José"), viewModel.uiState.value.availableProvinces)
          assertEquals(
              listOf("Escazú", "Grecia", "Moravia"),
              viewModel.uiState.value.availableCantons,
          )
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun searchQueryFiltersByCourtOrComplexName() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val viewModel = CourtCatalogViewModel(FakeCourtCatalogRepository(), this)
          advanceUntilIdle()

          viewModel.updateSearchQuery("moravia")
          assertEquals(listOf("court-2"), viewModel.uiState.value.visibleCourts.map { it.id })

          viewModel.updateSearchQuery("mejengas")
          assertEquals(listOf("court-1"), viewModel.uiState.value.visibleCourts.map { it.id })
        } finally {
          Dispatchers.resetMain()
        }
      }

  @Test
  fun selectingProvinceResetsUnavailableCantonAndNarrowsVisibleCourts() =
      runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        try {
          val viewModel = CourtCatalogViewModel(FakeCourtCatalogRepository(), this)
          advanceUntilIdle()

          viewModel.selectProvince("San José")
          assertEquals(
              listOf("court-1", "court-2"),
              viewModel.uiState.value.visibleCourts.map { it.id },
          )
          assertEquals(listOf("Escazú", "Moravia"), viewModel.uiState.value.availableCantons)

          viewModel.selectCanton("Escazú")
          assertEquals(listOf("court-1"), viewModel.uiState.value.visibleCourts.map { it.id })

          viewModel.selectProvince("Alajuela")
          assertNull(viewModel.uiState.value.selectedCanton)
          assertEquals(listOf("court-5"), viewModel.uiState.value.visibleCourts.map { it.id })
          assertEquals(listOf("Grecia"), viewModel.uiState.value.availableCantons)
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
              listOf("court-1", "court-2", "court-5"),
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
