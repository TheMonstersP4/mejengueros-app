package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PokemonListResponseDto(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<PokemonSummaryDto>,
)

@Serializable
data class PokemonSummaryDto(
    val name: String,
    val url: String,
)
