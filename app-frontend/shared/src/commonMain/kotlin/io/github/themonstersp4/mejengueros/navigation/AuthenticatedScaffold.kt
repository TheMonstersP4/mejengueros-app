package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosBottomNavigationBar
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosBottomNavigationItem
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosMobileScaffold
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTopAppBar

@Composable
fun AuthenticatedScaffold(
    selectedRoute: AuthenticatedTopLevelRoute,
    onHomeSelected: () -> Unit,
    onKitSelected: () -> Unit,
    onPokedexSelected: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    overlayVisible: Boolean = false,
    overlayContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
  var showSignOutConfirmation by rememberSaveable { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    MejenguerosMobileScaffold(
        modifier =
            Modifier.fillMaxSize()
                .then(if (overlayVisible) Modifier.clearAndSetSemantics {} else Modifier),
        topBar = {
          MejenguerosTopAppBar(
              title = "Mejengueros",
              navigationIcon = {
                onNavigateBack?.let { navigateBack ->
                  IconButton(onClick = navigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        modifier = Modifier.size(20.dp),
                    )
                  }
                }
              },
              actions = {
                IconButton(onClick = { showSignOutConfirmation = true }) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                      contentDescription = "Cerrar sesión",
                      modifier = Modifier.size(20.dp),
                  )
                }
              },
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
                          label = "Kit",
                          selected = selectedRoute == AuthenticatedTopLevelRoute.Kit,
                          onClick = onKitSelected,
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

    if (overlayVisible) {
      overlayContent()
    }

    if (showSignOutConfirmation) {
      AlertDialog(
          onDismissRequest = { showSignOutConfirmation = false },
          title = { Text("¿Cerrar sesión?") },
          text = { Text("Tendrás que iniciar sesión de nuevo para continuar.") },
          confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                  showSignOutConfirmation = false
                  onSignOut()
                }
            ) {
              Text("Cerrar sesión")
            }
          },
          dismissButton = {
            androidx.compose.material3.TextButton(onClick = { showSignOutConfirmation = false }) {
              Text("Cancelar")
            }
          },
      )
    }
  }
}

enum class AuthenticatedTopLevelRoute {
  Home,
  Kit,
  Pokedex,
}
