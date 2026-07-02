package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileEnvelopeDto(
    val success: Boolean,
    val data: UserProfileDto? = null,
)

@Serializable
data class UserProfileDto(
    val id: String,
    val roles: List<String> = emptyList(),
)
