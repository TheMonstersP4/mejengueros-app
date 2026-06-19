package io.github.themonstersp4.mejengueros.navigation

class LoginNavigationActions(
    val onSignedIn: () -> Unit,
    val openRegister: () -> Unit,
    val openVerification: () -> Unit,
    val closeAuthStep: () -> Unit,
    val backToLogin: () -> Unit,
)

class AuthenticatedShellActions(
    val selectHome: () -> Unit,
    val selectPokedex: () -> Unit,
    val signOut: () -> Unit,
)

class PokedexNavigationActions(
    val openPokemonDetail: (Int) -> Unit,
    val closeDetail: () -> Unit,
)
