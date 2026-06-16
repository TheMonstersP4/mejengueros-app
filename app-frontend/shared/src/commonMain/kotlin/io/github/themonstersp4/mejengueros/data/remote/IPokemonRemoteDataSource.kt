package io.github.themonstersp4.mejengueros.data.remote

import io.github.themonstersp4.mejengueros.domain.model.PokemonDetail
import io.github.themonstersp4.mejengueros.domain.model.PokemonPage

interface IPokemonRemoteDataSource {
  suspend fun getPokemonPage(limit: Int, offset: Int, query: String = ""): PokemonPage

  suspend fun getPokemonDetail(id: Int): PokemonDetail
}
