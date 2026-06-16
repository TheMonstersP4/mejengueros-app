package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.data.remote.dto.PokemonDetailDto
import io.github.themonstersp4.mejengueros.data.remote.dto.PokemonListResponseDto
import io.github.themonstersp4.mejengueros.data.remote.dto.PokemonSummaryDto
import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail
import io.github.themonstersp4.mejengueros.domain.model.PokemonPage
import io.github.themonstersp4.mejengueros.domain.model.PokemonSummary
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class PokemonRemoteDataSource(private val httpClient: HttpClient) : IPokemonRemoteDataSource {
  override suspend fun getPokemonPage(limit: Int, offset: Int, query: String): PokemonPage {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) {
      return getUnfilteredPokemonPage(limit = limit, offset = offset)
    }

    return getFilteredPokemonPage(
        limit = limit,
        offset = offset,
        query = normalizedQuery,
    )
  }

  override suspend fun getPokemonDetail(id: Int): PokemonDetail {
    val response = httpClient.get("https://pokeapi.co/api/v2/pokemon/$id").body<PokemonDetailDto>()
    return response.toDomain()
  }

  private suspend fun getUnfilteredPokemonPage(limit: Int, offset: Int): PokemonPage {
    val response = getPokemonListResponse(limit = limit, offset = offset)
    val items = response.results.mapNotNull { it.toDomainOrNull() }
    return PokemonPage(items = items, nextOffset = response.next?.extractOffset())
  }

  private suspend fun getFilteredPokemonPage(limit: Int, offset: Int, query: String): PokemonPage {
    val normalizedQuery = query.lowercase()
    val matches = mutableListOf<PokemonSummary>()
    var nextScanOffset: Int? = 0
    val requestedMatchCount = offset + limit

    while (matches.size < requestedMatchCount && nextScanOffset != null) {
      val response = getPokemonListResponse(limit = RemoteSearchPageSize, offset = nextScanOffset)
      val matchedItems =
          response.results
              .mapNotNull { it.toDomainOrNull() }
              .filter { pokemon ->
                pokemon.name.contains(normalizedQuery, ignoreCase = true) ||
                    pokemon.id.toString() == normalizedQuery
              }

      matches += matchedItems
      nextScanOffset = response.next?.extractOffset()
    }

    val items = matches.drop(offset).take(limit)
    val hasKnownMoreMatches = matches.size > requestedMatchCount
    val mayHaveMoreRemoteMatches = nextScanOffset != null

    return PokemonPage(
        items = items,
        nextOffset = if (hasKnownMoreMatches || mayHaveMoreRemoteMatches) offset + limit else null,
    )
  }

  private suspend fun getPokemonListResponse(limit: Int, offset: Int): PokemonListResponseDto =
      httpClient
          .get("https://pokeapi.co/api/v2/pokemon") {
            parameter("limit", limit)
            parameter("offset", offset)
          }
          .body<PokemonListResponseDto>()

  private fun PokemonSummaryDto.toDomainOrNull(): PokemonSummary? {
    val id = url.extractPokemonId() ?: return null
    return PokemonSummary(id = id, name = name, imageUrl = pokemonImageUrl(id))
  }

  private fun PokemonDetailDto.toDomain(): PokemonDetail {
    val imageUrl =
        sprites.other?.officialArtwork?.frontDefault ?: sprites.frontDefault ?: pokemonImageUrl(id)
    return PokemonDetail(
        id = id,
        name = name,
        height = height,
        weight = weight,
        imageUrl = imageUrl,
        types = types.sortedBy { it.slot }.map { it.type.name },
    )
  }

  private fun String.extractPokemonId(): Int? = trimEnd('/').substringAfterLast('/').toIntOrNull()

  private fun String.extractOffset(): Int? =
      substringAfter("offset=", missingDelimiterValue = "").substringBefore('&').toIntOrNull()

  private fun pokemonImageUrl(id: Int): String =
      "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"

  private companion object {
    const val RemoteSearchPageSize = 100
  }
}
