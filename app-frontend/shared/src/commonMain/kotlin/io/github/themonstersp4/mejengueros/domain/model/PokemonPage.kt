package io.github.themonstersp4.mejengueros.domain.model

data class PokemonPage(
    val items: List<PokemonSummary>,
    val nextOffset: Int?,
)
