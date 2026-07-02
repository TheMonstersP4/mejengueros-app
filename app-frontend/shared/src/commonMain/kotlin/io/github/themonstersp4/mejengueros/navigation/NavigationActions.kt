package io.github.themonstersp4.mejengueros.navigation

class LoginNavigationActions(
    val onSignedIn: () -> Unit,
    val openRegister: () -> Unit,
    val openVerification: () -> Unit,
    val openForgotPassword: () -> Unit,
    val openPasswordReset: () -> Unit,
    val closeAuthStep: () -> Unit,
    val backToLogin: () -> Unit,
)

class AuthenticatedShellActions(
    val selectSearch: () -> Unit,
    val selectReservations: () -> Unit,
    val selectNotifications: () -> Unit,
    val selectMyComplex: () -> Unit,
    val returnToMyComplexRoot: () -> Unit,
    val openCatalogCourtDetail: (CatalogCourtDetailRoute) -> Unit,
    val openCatalogReservation: (CatalogReservationRoute) -> Unit,
    val openComplexDetail: (String) -> Unit,
    val openAddCourt: (String, String) -> Unit,
    val openCreateComplex: () -> Unit,
    val openCourtAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
    val closeAddCourtAfterSuccess: () -> Unit,
    val closeCurrentDetail: () -> Unit,
    val signOut: () -> Unit,
    val refreshOwnerRole: () -> Unit,
    // Switches an owner to the mejenguero (player) bottom-nav shell, landing on Buscar.
    val switchToPlayerView: () -> Unit = {},
    // Switches an owner back to the owner drawer shell, landing on Mi complejo.
    val switchToOwnerView: () -> Unit = {},
    val isOwner: Boolean = false,
    // True when an owner is temporarily browsing in player mode.
    val viewingAsPlayer: Boolean = false,
)
