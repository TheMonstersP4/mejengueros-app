package io.github.themonstersp4.mejengueros.domain.model

data class PokemonSummary(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val isFavorite: Boolean = false,
) {
  val displayName: String = name.replaceFirstChar { it.uppercase() }
}
