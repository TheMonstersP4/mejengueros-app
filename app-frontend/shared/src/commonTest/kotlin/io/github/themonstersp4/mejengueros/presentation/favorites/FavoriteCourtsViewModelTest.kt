package io.github.themonstersp4.mejengueros.presentation.favorites

import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogPage
import io.github.themonstersp4.mejengueros.domain.repository.ICourtCatalogRepository
import io.github.themonstersp4.mejengueros.domain.repository.ICourtFavoriteRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteCourtsViewModelTest {
  @Test
  fun emptyIdsAreEmptyWithoutCatalogRequestAndUserIdIsScoped() = runTest {
    val favorites = FakeFavorites(emptyList())
    val catalog = FakeCatalog()
    val viewModel = FavoriteCourtsViewModel("user-a", favorites, catalog, backgroundScope)
    runCurrent()
    assertFalse(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.courts.isEmpty())
    assertEquals(listOf("user-a"), favorites.observedUsers)
    assertTrue(catalog.requests.isEmpty())
  }

  @Test
  fun loadsContentInObservedIdOrderAndRetriesLoadError() = runTest {
    val favorites = FakeFavorites(listOf("b", "a"))
    val catalog = FakeCatalog(throwOnNext = true)
    val viewModel = FavoriteCourtsViewModel("user-a", favorites, catalog, backgroundScope)
    runCurrent()
    assertEquals(
        "No pudimos cargar tus canchas favoritas. Intentá nuevamente.",
        viewModel.uiState.value.errorMessage,
    )
    viewModel.retry()
    runCurrent()
    assertEquals(listOf("b", "a"), viewModel.uiState.value.courts.map { it.id })
    assertEquals(listOf(listOf("b", "a"), listOf("b", "a")), catalog.requests)
  }

  @Test
  fun removeIsPessimisticBlocksDoubleTapAndKeepsItemOnFailure() = runTest {
    val favorites = FakeFavorites(listOf("a"), failRemove = true)
    val viewModel = FavoriteCourtsViewModel("user-a", favorites, FakeCatalog(), backgroundScope)
    runCurrent()
    viewModel.remove("a")
    viewModel.remove("a")
    runCurrent()
    assertEquals(listOf(Triple("user-a", "a", false)), favorites.setCalls)
    assertEquals(listOf("a"), viewModel.uiState.value.courts.map { it.id })
    assertEquals(
        "No pudimos quitar la cancha. Intentá nuevamente.",
        viewModel.uiState.value.removalErrorMessage,
    )
  }

  @Test
  fun removeSuccessReliesOnReactiveIdsAndTransitionsToEmpty() = runTest {
    val favorites = FakeFavorites(listOf("a"))
    val viewModel = FavoriteCourtsViewModel("user-a", favorites, FakeCatalog(), backgroundScope)
    runCurrent()
    viewModel.remove("a")
    runCurrent()
    assertTrue(viewModel.uiState.value.courts.isEmpty())
    assertEquals(listOf(Triple("user-a", "a", false)), favorites.setCalls)
  }

  @Test
  fun latestFavoriteIdsCancelPreviousHydrationWithoutStaleOverwrite() = runTest {
    val favorites = FakeFavorites(listOf("old"))
    val catalog = BlockingCatalog()
    val viewModel = FavoriteCourtsViewModel("user-a", favorites, catalog, backgroundScope)
    runCurrent()
    favorites.ids.value = listOf("new")
    runCurrent()
    catalog.newResult.complete(listOf(testCourt("new")))
    runCurrent()
    assertEquals(listOf("new"), viewModel.uiState.value.courts.map { it.id })
    assertTrue(catalog.oldRequestCancelled)
  }

  @Test
  fun observerFailureRetriesWithOneReplacementObservation() = runTest {
    val favorites = FailingThenEmittingFavorites()
    val viewModel = FavoriteCourtsViewModel("user-a", favorites, FakeCatalog(), backgroundScope)
    runCurrent()

    assertFalse(viewModel.uiState.value.isLoading)
    assertEquals(
        "No pudimos cargar tus canchas favoritas. Intentá nuevamente.",
        viewModel.uiState.value.errorMessage,
    )
    assertEquals(1, favorites.observationCount)

    viewModel.retry()
    runCurrent()

    assertEquals(listOf("a"), viewModel.uiState.value.courts.map { it.id })
    assertEquals(2, favorites.observationCount)
    assertEquals(1, favorites.activeObservations)
  }

  @Test
  fun retryCannotOverwriteNewerFavoriteIds() = runTest {
    val favorites = FakeFavorites(listOf("old"))
    val catalog = RetryBlockingCatalog()
    val viewModel = FavoriteCourtsViewModel("user-a", favorites, catalog, backgroundScope)
    runCurrent()

    viewModel.retry()
    runCurrent()
    favorites.ids.value = listOf("new")
    runCurrent()

    catalog.newResult.complete(listOf(testCourt("new")))
    runCurrent()
    catalog.oldRetryResult.complete(listOf(testCourt("old")))
    runCurrent()

    assertEquals(listOf("new"), viewModel.uiState.value.courts.map { it.id })
  }

  @Test
  fun successfulRemovalClearsItsGuardWhileOtherPendingRemovalRemainsBlocked() = runTest {
    val favorites = BlockingRemovalFavorites(listOf("a", "b"))
    val viewModel = FavoriteCourtsViewModel("user-a", favorites, FakeCatalog(), backgroundScope)
    runCurrent()

    viewModel.remove("a")
    viewModel.remove("b")
    runCurrent()
    favorites.completeRemoval("a")
    runCurrent()

    assertFalse("a" in viewModel.uiState.value.removingCourtIds)
    assertTrue("b" in viewModel.uiState.value.removingCourtIds)

    viewModel.remove("b")
    runCurrent()

    assertEquals(
        listOf(Triple("user-a", "a", false), Triple("user-a", "b", false)),
        favorites.setCalls,
    )
    assertTrue("b" in viewModel.uiState.value.removingCourtIds)

    favorites.ids.value = listOf("a", "b")
    runCurrent()
    viewModel.remove("a")
    runCurrent()

    assertEquals(
        listOf(
            Triple("user-a", "a", false),
            Triple("user-a", "b", false),
            Triple("user-a", "a", false),
        ),
        favorites.setCalls,
    )
    assertTrue("b" in viewModel.uiState.value.removingCourtIds)
  }

  private class FakeFavorites(ids: List<String>, private val failRemove: Boolean = false) :
      ICourtFavoriteRepository {
    val ids = MutableStateFlow(ids)
    val observedUsers = mutableListOf<String>()
    val setCalls = mutableListOf<Triple<String, String, Boolean>>()

    override fun observeFavoriteCourtIds(userId: String): Flow<List<String>> {
      observedUsers += userId
      return ids
    }

    override suspend fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) {
      setCalls += Triple(userId, courtId, isFavorite)
      if (failRemove) error("failed") else ids.value = ids.value - courtId
    }

    override suspend fun isFavorite(userId: String, courtId: String) = courtId in ids.value
  }

  private class FakeCatalog(private var throwOnNext: Boolean = false) : ICourtCatalogRepository {
    val requests = mutableListOf<List<String>>()

    override suspend fun getFavoriteCourts(courtIds: List<String>): List<CourtCatalogItem> {
      requests += courtIds
      if (throwOnNext) {
        throwOnNext = false
        error("offline")
      }
      return courtIds.map(::testCourt)
    }

    override suspend fun getCatalogCourts(
        searchQuery: String?,
        provinceId: String?,
        cantonId: String?,
        serviceIds: List<String>,
        courtIds: List<String>,
        minRating: Int?,
        page: Int,
        pageSize: Int,
    ): CourtCatalogPage = error("unused")
  }

  private class FailingThenEmittingFavorites : ICourtFavoriteRepository {
    var observationCount = 0
    var activeObservations = 0

    override fun observeFavoriteCourtIds(userId: String): Flow<List<String>> = flow {
      observationCount += 1
      activeObservations += 1
      try {
        if (observationCount == 1) error("observer failed") else emit(listOf("a"))
      } finally {
        if (observationCount == 1) activeObservations -= 1
      }
    }

    override suspend fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) = Unit

    override suspend fun isFavorite(userId: String, courtId: String) = false
  }

  private class BlockingCatalog : ICourtCatalogRepository {
    val newResult = CompletableDeferred<List<CourtCatalogItem>>()
    var oldRequestCancelled = false

    override suspend fun getFavoriteCourts(courtIds: List<String>): List<CourtCatalogItem> =
        if (courtIds == listOf("old")) {
          try {
            CompletableDeferred<List<CourtCatalogItem>>().await()
          } finally {
            oldRequestCancelled = true
          }
        } else {
          newResult.await()
        }

    override suspend fun getCatalogCourts(
        searchQuery: String?,
        provinceId: String?,
        cantonId: String?,
        serviceIds: List<String>,
        courtIds: List<String>,
        minRating: Int?,
        page: Int,
        pageSize: Int,
    ): CourtCatalogPage = error("unused")
  }

  private class RetryBlockingCatalog : ICourtCatalogRepository {
    val oldRetryResult = CompletableDeferred<List<CourtCatalogItem>>()
    val newResult = CompletableDeferred<List<CourtCatalogItem>>()
    private var oldRequests = 0

    override suspend fun getFavoriteCourts(courtIds: List<String>): List<CourtCatalogItem> =
        when (courtIds) {
          listOf("old") -> {
            oldRequests += 1
            if (oldRequests == 1) error("initial load failure")
            oldRetryResult.await()
          }
          listOf("new") -> newResult.await()
          else -> error("unexpected ids")
        }

    override suspend fun getCatalogCourts(
        searchQuery: String?,
        provinceId: String?,
        cantonId: String?,
        serviceIds: List<String>,
        courtIds: List<String>,
        minRating: Int?,
        page: Int,
        pageSize: Int,
    ): CourtCatalogPage = error("unused")
  }

  private class BlockingRemovalFavorites(ids: List<String>) : ICourtFavoriteRepository {
    val ids = MutableStateFlow(ids)
    val setCalls = mutableListOf<Triple<String, String, Boolean>>()
    private val removalResults = mutableMapOf<String, CompletableDeferred<Unit>>()

    override fun observeFavoriteCourtIds(userId: String): Flow<List<String>> = ids

    override suspend fun setFavorite(userId: String, courtId: String, isFavorite: Boolean) {
      setCalls += Triple(userId, courtId, isFavorite)
      removalResults.getOrPut(courtId) { CompletableDeferred() }.await()
      ids.value = ids.value - courtId
    }

    override suspend fun isFavorite(userId: String, courtId: String) = courtId in ids.value

    fun completeRemoval(courtId: String) {
      removalResults.getOrPut(courtId) { CompletableDeferred() }.complete(Unit)
    }
  }

  private fun court(id: String) = testCourt(id)

  private companion object {
    fun testCourt(id: String) =
        CourtCatalogItem(
            id,
            "complex",
            "Complex",
            "Court $id",
            "p",
            "Province",
            "c",
            "Canton",
            services = emptyList(),
            ratingAverage = null,
            ratingCount = 0,
            imageUrl = null,
            isReservableToday = false,
        )
  }
}
