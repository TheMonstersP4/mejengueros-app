package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtCatalogRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class CourtCatalogRepositoryTest {
  @Test
  fun getCatalogCourtsDelegatesToRemoteDataSource() = runTest {
    val remoteDataSource = FakeCourtCatalogRemoteDataSource()
    val repository = CourtCatalogRepository(remoteDataSource)

    val page =
        repository.getCatalogCourts("nogales", "province-id", "canton-id", page = 2, pageSize = 20)

    assertEquals(
        listOf(CatalogRequest("nogales", "province-id", "canton-id", 2, 20)),
        remoteDataSource.requests,
    )
    assertEquals(listOf(fakeCourt), page.items)
    assertEquals(2, page.page)
  }

  private data class CatalogRequest(
      val searchQuery: String?,
      val provinceId: String?,
      val cantonId: String?,
      val page: Int,
      val pageSize: Int,
  )

  private class FakeCourtCatalogRemoteDataSource : ICourtCatalogRemoteDataSource {
    val requests = mutableListOf<CatalogRequest>()

    override suspend fun getCatalogCourts(
        searchQuery: String?,
        provinceId: String?,
        cantonId: String?,
        page: Int,
        pageSize: Int,
    ): CourtCatalogPage {
      requests += CatalogRequest(searchQuery, provinceId, cantonId, page, pageSize)
      return CourtCatalogPage(
          items = listOf(fakeCourt),
          page = page,
          pageSize = pageSize,
          totalItems = 1,
          totalPages = page,
      )
    }
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
  }
}
