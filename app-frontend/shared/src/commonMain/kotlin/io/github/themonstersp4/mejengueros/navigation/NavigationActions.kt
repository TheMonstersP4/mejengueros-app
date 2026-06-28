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
    val openCreateComplex: () -> Unit,
    val openCourtAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
    val closeCurrentDetail: () -> Unit,
    val signOut: () -> Unit,
)
