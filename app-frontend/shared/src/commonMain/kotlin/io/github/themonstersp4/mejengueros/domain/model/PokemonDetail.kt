package io.github.themonstersp4.mejengueros.domain.model

import kotlin.math.absoluteValue

data class PokemonDetail(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val imageUrl: String,
    val types: List<String>,
    val isFavorite: Boolean = false,
) {
  val displayName: String = name.replaceFirstChar { it.uppercase() }
  val displayHeight: String = "${height.toTenthsDecimal()} m"
  val displayWeight: String = "${weight.toTenthsDecimal()} kg"
}

private fun Int.toTenthsDecimal(): String {
  val whole = this / 10
  val fraction = (this % 10).absoluteValue
  return "$whole.$fraction"
}
