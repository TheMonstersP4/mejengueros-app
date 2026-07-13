package io.github.themonstersp4.mejengueros.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable sealed interface AppRoute : NavKey

internal const val UnspecifiedCatalogReservationAttemptId = 0L

@Serializable data object LoginRoute : AppRoute

@Serializable data object RegisterRoute : AppRoute

@Serializable data object VerifyAccountRoute : AppRoute

@Serializable data object ForgotPasswordRoute : AppRoute

@Serializable data object ResetPasswordRoute : AppRoute

@Serializable data object HomeRoute : AppRoute

@Serializable data object SearchRoute : AppRoute

@Serializable
data class CatalogCourtDetailRoute(
    val courtId: String,
    val complexId: String,
    val complexName: String,
    val courtName: String,
    val provinceName: String = "",
    val cantonName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val services: List<String> = emptyList(),
    val ratingAverage: Double? = null,
    val ratingCount: Int = 0,
    val imageUrl: String? = null,
    val isReservableToday: Boolean = false,
) : AppRoute

@Serializable
data class CatalogReservationRoute(
    val courtId: String,
    val complexId: String,
    val complexName: String,
    val courtName: String,
    val provinceName: String = "",
    val cantonName: String = "",
    val attemptId: Long = UnspecifiedCatalogReservationAttemptId,
) : AppRoute

@Serializable data object ReservationsRoute : AppRoute

@Serializable data object NotificationsRoute : AppRoute

@Serializable data object MyComplexRoute : AppRoute

@Serializable data class ComplexDetailRoute(val complexId: String) : AppRoute

@Serializable data class AddCourtRoute(val complexId: String, val complexName: String) : AppRoute

@Serializable data object CreateComplexRoute : AppRoute

@Serializable data object KitRoute : AppRoute

@Serializable data object AvailabilitySelectorsRoute : AppRoute

@Serializable
data class CourtAvailabilityRoute(
    val courtId: String,
    val courtName: String,
    val complexName: String,
) : AppRoute

@Serializable data object OwnerReceivedReviewsRoute : AppRoute

@Serializable data object PokedexRoute : AppRoute

@Serializable data class PokemonDetailRoute(val id: Int) : AppRoute
