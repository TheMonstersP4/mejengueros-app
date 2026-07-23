package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.CourtCatalogEnvelopeDto
import io.github.themonstersp4.mejengueros.data.remote.dto.ServiceCatalogEnvelopeDto
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogPage
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
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
      serviceIds: List<String>,
      page: Int,
      pageSize: Int,
  ): CourtCatalogPage {
    return try {
      val envelope =
          httpClient
              .get("/v1/courts/catalog") {
                searchQuery?.trim()?.takeIf { it.isNotEmpty() }?.let { parameter("q", it) }
                provinceId?.let { parameter("provinceId", it) }
                cantonId?.let { parameter("cantonId", it) }
                // Repeated query param (serviceIds=a&serviceIds=b) so the backend
                // can require the court to offer every selected service.
                serviceIds.forEach { parameter("serviceIds", it) }
                parameter("page", page)
                parameter("pageSize", pageSize)
              }
              .body<CourtCatalogEnvelopeDto>()

      val items =
          envelope.data.map { court ->
            CourtCatalogItem(
                id = court.courtId,
                complexId = court.complexId,
                complexName = court.complexName,
                courtName = court.courtName,
                provinceId = court.province.id,
                provinceName = court.province.name,
                cantonId = court.canton.id,
                cantonName = court.canton.name,
                latitude = court.latitude,
                longitude = court.longitude,
                services = court.services,
                ratingAverage = court.rating.average,
                ratingCount = court.rating.count,
                imageUrl = court.imageUrl,
                isReservableToday = court.isReservableToday,
            )
          }

      // Fall back to the requested window when the server omits pagination
      // metadata so the catalog never loops asking for a next page that the
      // response cannot describe.
      val pagination = envelope.meta.pagination
      CourtCatalogPage(
          items = items,
          page = pagination?.page ?: page,
          pageSize = pagination?.pageSize ?: pageSize,
          totalItems = pagination?.totalItems ?: items.size,
          totalPages = pagination?.totalPages ?: if (items.isEmpty()) 0 else page,
      )
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }

  override suspend fun getServiceCatalog(): List<ServiceCatalogItem> {
    return try {
      // No scope filter: the catalog filter needs every active service because a
      // court can offer both complex-scoped and court-scoped services.
      httpClient.get("/v1/services").body<ServiceCatalogEnvelopeDto>().data.map { service ->
        ServiceCatalogItem(id = service.id, name = service.name, scope = service.scope)
      }
    } catch (error: ResponseException) {
      throw error.toAppApiException(json)
    }
  }
}
