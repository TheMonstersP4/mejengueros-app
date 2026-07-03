package io.github.themonstersp4.mejengueros.data.remote.dto

import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import kotlinx.serialization.Serializable

@Serializable
data class MyComplexHubEnvelopeDto(
    val success: Boolean,
    val data: MyComplexHubResponseDto? = null,
)

@Serializable
data class MyComplexHubResponseDto(
    val complexes: List<MyComplexHubComplexDto> = emptyList(),
)

@Serializable
data class MyComplexHubComplexDto(
    val id: String,
    val name: String,
    val address: String,
    val provinceId: String? = null,
    val cantonId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String,
    val courts: List<MyComplexHubCourtDto> = emptyList(),
)

@Serializable
data class MyComplexHubCourtDto(
    val id: String,
    val name: String,
    val status: String,
    val availabilityStatus: CourtAvailabilitySetupStatus,
    val imageUrl: String? = null,
)

@Serializable
data class UpdateCourtImageRequestDto(
    val imageUploadId: String,
)

@Serializable
data class UpdateCourtImageEnvelopeDto(
    val success: Boolean,
    val data: UpdateCourtImageEnvelopeDataDto? = null,
)

@Serializable
data class UpdateCourtImageEnvelopeDataDto(
    val court: MyComplexHubCourtDto,
)
