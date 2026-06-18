package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosBottomNavigationBar
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosBottomNavigationItem
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosMobileScaffold
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTopAppBar

@Composable
fun AuthenticatedScaffold(
    selectedRoute: AuthenticatedTopLevelRoute,
    onHomeSelected: () -> Unit,
    onPokedexSelected: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
  MejenguerosMobileScaffold(
      modifier = modifier,
      topBar = {
        MejenguerosTopAppBar(
            title = "Mejengueros",
            navigationIcon = {
              onNavigateBack?.let { navigateBack ->
                TextButton(onClick = navigateBack) { Text("‹ Back") }
              }
            },
            actions = { TextButton(onClick = onSignOut) { Text("Sign out") } },
        )
      },
      bottomBar = {
        MejenguerosBottomNavigationBar(
            items =
                listOf(
                    MejenguerosBottomNavigationItem(
                        label = "Home",
                        selected = selectedRoute == AuthenticatedTopLevelRoute.Home,
                        onClick = onHomeSelected,
                    ),
                    MejenguerosBottomNavigationItem(
                        label = "Pokédex",
                        selected = selectedRoute == AuthenticatedTopLevelRoute.Pokedex,
                        onClick = onPokedexSelected,
                    ),
                )
        )
      },
      content = content,
  )
}

enum class AuthenticatedTopLevelRoute {
  Home,
  Pokedex,
}
