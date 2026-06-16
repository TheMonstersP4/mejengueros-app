package io.github.themonstersp4.mejengueros.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PokemonDetailDto(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val sprites: PokemonSpritesDto = PokemonSpritesDto(),
    val types: List<PokemonTypeSlotDto> = emptyList(),
)

@Serializable
data class PokemonSpritesDto(
    @SerialName("front_default") val frontDefault: String? = null,
    val other: PokemonOtherSpritesDto? = null,
)

@Serializable
data class PokemonOtherSpritesDto(
    @SerialName("official-artwork") val officialArtwork: PokemonOfficialArtworkDto? = null,
)

@Serializable
data class PokemonOfficialArtworkDto(
    @SerialName("front_default") val frontDefault: String? = null,
)

@Serializable
data class PokemonTypeSlotDto(
    val slot: Int,
    val type: PokemonTypeDto,
)

@Serializable
data class PokemonTypeDto(
    val name: String,
)
