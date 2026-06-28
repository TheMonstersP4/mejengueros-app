package io.github.themonstersp4.mejengueros.data.remote.dto

import io.github.themonstersp4.mejengueros.domain.model.ServiceScope
import kotlinx.serialization.Serializable

@Serializable
data class CreateComplexRequestDto(
    val complex: CreateComplexRequestPayloadDto,
    val firstCourt: CreateCourtRequestPayloadDto,
)

@Serializable
data class CreateComplexRequestPayloadDto(
    val name: String,
    val provinceId: String,
    val cantonId: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val serviceIds: List<String>,
)

@Serializable
data class CreateCourtRequestPayloadDto(val name: String, val serviceIds: List<String>)

@Serializable data class CreateCourtEnvelopeDto(val success: Boolean, val data: CreatedCourtDto?)

@Serializable
data class ProvinceCatalogEnvelopeDto(
    val success: Boolean,
    val data: List<ProvinceCatalogItemDto> = emptyList(),
)

@Serializable
data class ProvinceCatalogItemDto(
    val id: String,
    val code: String,
    val name: String,
)

@Serializable
data class CantonCatalogEnvelopeDto(
    val success: Boolean,
    val data: List<CantonCatalogItemDto> = emptyList(),
)

@Serializable
data class CantonCatalogItemDto(
    val id: String,
    val provinceId: String,
    val code: String,
    val name: String,
)

@Serializable
data class ServiceCatalogEnvelopeDto(
    val success: Boolean,
    val data: List<ServiceCatalogItemDto> = emptyList(),
)

@Serializable
data class ServiceCatalogItemDto(
    val id: String,
    val name: String,
    val scope: ServiceScope,
)

@Serializable
data class CreateComplexEnvelopeDto(val success: Boolean, val data: CreateComplexResponseDto?)

@Serializable
data class CreateComplexResponseDto(
    val complex: CreatedComplexDto,
    val firstCourt: CreatedCourtDto,
)

@Serializable
data class CreatedComplexDto(
    val id: String,
    val name: String,
    val address: String,
)

@Serializable
data class CreatedCourtDto(
    val id: String,
    val complexId: String,
    val name: String,
)
