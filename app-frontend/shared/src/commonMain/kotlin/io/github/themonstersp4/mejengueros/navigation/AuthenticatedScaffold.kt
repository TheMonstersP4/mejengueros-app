package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
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
  Scaffold(
      modifier = modifier,
      topBar = {
        TopAppBar(
            title = { Text("Mejengueros") },
            navigationIcon = {
              onNavigateBack?.let { navigateBack ->
                TextButton(onClick = navigateBack) { Text("‹ Back") }
              }
            },
            actions = { TextButton(onClick = onSignOut) { Text("Sign out") } },
        )
      },
      bottomBar = {
        NavigationBar {
          NavigationBarItem(
              selected = selectedRoute == AuthenticatedTopLevelRoute.Home,
              onClick = onHomeSelected,
              label = { Text("Home") },
              icon = {},
          )
          NavigationBarItem(
              selected = selectedRoute == AuthenticatedTopLevelRoute.Pokedex,
              onClick = onPokedexSelected,
              label = { Text("Pokédex") },
              icon = {},
          )
        }
      },
      content = content,
  )
}

enum class AuthenticatedTopLevelRoute {
  Home,
  Pokedex,
}
