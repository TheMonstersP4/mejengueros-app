package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateComplexRequestDto(
    val complex: CreateComplexRequestPayloadDto,
    val firstCourt: CreateCourtRequestPayloadDto,
)

@Serializable
data class CreateComplexRequestPayloadDto(
    val name: String,
    val address: String,
)

@Serializable data class CreateCourtRequestPayloadDto(val name: String)

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
