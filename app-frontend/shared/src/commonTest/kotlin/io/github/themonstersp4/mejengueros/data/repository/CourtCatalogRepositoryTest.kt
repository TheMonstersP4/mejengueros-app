package io.github.themonstersp4.mejengueros.data.repository

import io.github.themonstersp4.mejengueros.data.remote.ICourtCatalogRemoteDataSource
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class CourtCatalogRepositoryTest {
  @Test
  fun getCatalogCourtsDelegatesToRemoteDataSource() = runTest {
    val remoteDataSource = FakeCourtCatalogRemoteDataSource()
    val repository = CourtCatalogRepository(remoteDataSource)

    val courts = repository.getCatalogCourts("nogales", "province-id", "canton-id")

    assertEquals(
        listOf<Triple<String?, String?, String?>>(Triple("nogales", "province-id", "canton-id")),
        remoteDataSource.requests,
    )
    assertEquals(listOf(fakeCourt), courts)
  }

  private class FakeCourtCatalogRemoteDataSource : ICourtCatalogRemoteDataSource {
    val requests = mutableListOf<Triple<String?, String?, String?>>()

    override suspend fun getCatalogCourts(
        searchQuery: String?,
        provinceId: String?,
        cantonId: String?,
    ): List<CourtCatalogItem> {
      requests += Triple(searchQuery, provinceId, cantonId)
      return listOf(fakeCourt)
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
