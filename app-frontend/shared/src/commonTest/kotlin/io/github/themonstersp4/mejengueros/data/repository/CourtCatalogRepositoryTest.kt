package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtCatalogRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogPage
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class CourtCatalogRepositoryTest {
  @Test
  fun getCatalogCourtsDelegatesToRemoteDataSource() = runTest {
    val remoteDataSource = FakeCourtCatalogRemoteDataSource()
    val repository = CourtCatalogRepository(remoteDataSource)

    val page =
        repository.getCatalogCourts(
            "nogales",
            "province-id",
            "canton-id",
            listOf("service-a", "service-b"),
            minRating = 4,
            page = 2,
            pageSize = 20,
        )

    assertEquals(
        listOf(
            CatalogRequest(
                "nogales",
                "province-id",
                "canton-id",
                listOf("service-a", "service-b"),
                emptyList(),
                4,
                2,
                20,
            )
        ),
        remoteDataSource.requests,
    )
    assertEquals(listOf(fakeCourt), page.items)
    assertEquals(2, page.page)
  }

  @Test
  fun getServiceCatalogDelegatesToRemoteDataSource() = runTest {
    val remoteDataSource = FakeCourtCatalogRemoteDataSource()
    val repository = CourtCatalogRepository(remoteDataSource)

    val services = repository.getServiceCatalog()

    assertEquals(listOf(fakeService), services)
  }

  @Test
  fun getFavoriteCourtsBatchesStableUniqueIdsAndRecomposesTheLocalOrder() = runTest {
    val remoteDataSource = FakeCourtCatalogRemoteDataSource()
    val repository = CourtCatalogRepository(remoteDataSource)
    val requestedIds = (1..21).map { "court-$it" }

    val result = repository.getFavoriteCourts(listOf("court-3", "court-1") + requestedIds)

    assertEquals(
        listOf(
            (listOf("court-3", "court-1") + requestedIds).distinct().take(20),
            listOf("court-21"),
        ),
        remoteDataSource.requests.map { it.courtIds },
    )
    assertEquals(listOf("court-3", "court-1", "court-2", "court-21"), result.map { it.id })
  }

  @Test
  fun getFavoriteCourtsPropagatesAnyBatchFailure() = runTest {
    val remoteDataSource = FakeCourtCatalogRemoteDataSource(failOnRequest = 2)
    val repository = CourtCatalogRepository(remoteDataSource)

    assertFailsWith<IllegalStateException> {
      repository.getFavoriteCourts((1..21).map { "court-$it" })
    }
  }

  private data class CatalogRequest(
      val searchQuery: String?,
      val provinceId: String?,
      val cantonId: String?,
      val serviceIds: List<String>,
      val courtIds: List<String>,
      val minRating: Int?,
      val page: Int,
      val pageSize: Int,
  )

  private class FakeCourtCatalogRemoteDataSource(
      private val failOnRequest: Int? = null,
  ) : ICourtCatalogRemoteDataSource {
    val requests = mutableListOf<CatalogRequest>()

    override suspend fun getCatalogCourts(
        searchQuery: String?,
        provinceId: String?,
        cantonId: String?,
        serviceIds: List<String>,
        courtIds: List<String>,
        minRating: Int?,
        page: Int,
        pageSize: Int,
    ): CourtCatalogPage {
      val request =
          CatalogRequest(
              searchQuery,
              provinceId,
              cantonId,
              serviceIds,
              courtIds,
              minRating,
              page,
              pageSize,
          )
      requests += request
      if (requests.size == failOnRequest) error("batch failed")
      return CourtCatalogPage(
          items =
              if (courtIds.isEmpty()) {
                listOf(fakeCourt)
              } else {
                courtIds.mapNotNull { id ->
                  when (id) {
                    "court-1",
                    "court-2",
                    "court-3",
                    "court-21" -> fakeCourt.copy(id = id)
                    else -> null
                  }
                }
              },
          page = page,
          pageSize = pageSize,
          totalItems = courtIds.size,
          totalPages = page,
      )
    }

    override suspend fun getServiceCatalog(): List<ServiceCatalogItem> = listOf(fakeService)
  }

  private companion object {
    val fakeCourt =
        CourtCatalogItem(
            id = "court-id",
            complexId = "complex-id",
            complexName = "Complejo Los Nogales",
            courtName = "Cancha 1",
            provinceId = "province-id",
            provinceName = "San José",
            cantonId = "canton-id",
            cantonName = "Escazú",
            services = listOf("Sintetico", "Iluminacion"),
            ratingAverage = 4.5,
            ratingCount = 2,
            imageUrl = null,
            isReservableToday = true,
        )

    val fakeService =
        ServiceCatalogItem(id = "service-id", name = "Sintetico", scope = ServiceScope.COURT)
  }
}
