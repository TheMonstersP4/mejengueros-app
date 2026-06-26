package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CourtCatalogEnvelopeDto(
    val success: Boolean,
    val data: List<CourtCatalogItemDto> = emptyList(),
)

@Serializable
data class CourtCatalogItemDto(
    val courtId: String,
    val courtName: String,
    val complexId: String,
    val complexName: String,
    val province: CourtCatalogLocationDto,
    val canton: CourtCatalogLocationDto,
    val services: List<String> = emptyList(),
    val rating: CourtCatalogRatingDto,
    val isReservableToday: Boolean,
    val imageUrl: String? = null,
)

@Serializable
data class CourtCatalogLocationDto(
    val id: String,
    val name: String,
)

@Serializable
data class CourtCatalogRatingDto(
    val average: Double? = null,
    val count: Int,
)
