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
    val selectHome: () -> Unit,
    val returnToHomeRoot: () -> Unit,
    val openCreateComplex: () -> Unit,
    val openCourtAvailability: (String, String, String) -> Unit,
    val selectKit: () -> Unit,
    val openAvailabilitySelectors: () -> Unit,
    val closeCurrentDetail: () -> Unit,
    val selectPokedex: () -> Unit,
    val signOut: () -> Unit,
)

class PokedexNavigationActions(
    val openPokemonDetail: (Int) -> Unit,
    val closeDetail: () -> Unit,
)
