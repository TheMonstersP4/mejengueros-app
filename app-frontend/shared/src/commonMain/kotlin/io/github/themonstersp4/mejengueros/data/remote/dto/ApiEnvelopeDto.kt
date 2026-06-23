package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
    val status: Int,
)

@Serializable
data class ApiErrorEnvelopeDto(
    val success: Boolean,
    val errors: List<ApiErrorDto> = emptyList(),
)
