package io.github.themonstersp4.mejengueros.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable sealed interface AppRoute : NavKey

@Serializable data object LoginRoute : AppRoute

@Serializable data object RegisterRoute : AppRoute

@Serializable data object VerifyAccountRoute : AppRoute

@Serializable data object HomeRoute : AppRoute

@Serializable data object KitRoute : AppRoute

@Serializable data object AvailabilitySelectorsRoute : AppRoute

@Serializable data object PokedexRoute : AppRoute

@Serializable data class PokemonDetailRoute(val id: Int) : AppRoute
