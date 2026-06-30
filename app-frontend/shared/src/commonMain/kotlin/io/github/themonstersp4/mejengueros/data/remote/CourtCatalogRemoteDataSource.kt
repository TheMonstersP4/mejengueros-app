package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.CourtCatalogEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.Json

class CourtCatalogRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
) : ICourtCatalogRemoteDataSource {
  override suspend fun getCatalogCourts(
      searchQuery: String?,
      provinceId: String?,
      cantonId: String?,
  ): List<CourtCatalogItem> {
    return try {
      httpClient
          .get("/v1/courts/catalog") {
            searchQuery?.trim()?.takeIf { it.isNotEmpty() }?.let { parameter("q", it) }
            provinceId?.let { parameter("provinceId", it) }
            cantonId?.let { parameter("cantonId", it) }
          }
          .body<CourtCatalogEnvelopeDto>()
          .data
          .map { court ->
            CourtCatalogItem(
                id = court.courtId,
                complexId = court.complexId,
                complexName = court.complexName,
                courtName = court.courtName,
                provinceId = court.province.id,
                provinceName = court.province.name,
                cantonId = court.canton.id,
                cantonName = court.canton.name,
                services = court.services,
                ratingAverage = court.rating.average,
                ratingCount = court.rating.count,
                imageUrl = court.imageUrl,
                isReservableToday = court.isReservableToday,
            )
          }
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}
