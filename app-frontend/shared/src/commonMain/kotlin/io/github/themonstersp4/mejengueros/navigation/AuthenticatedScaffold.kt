package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosConfirmationDialog
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosMobileScaffold
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTopAppBar

@Composable
fun AuthenticatedScaffold(
    selectedRoute: AuthenticatedTopLevelRoute,
    onSearchSelected: () -> Unit,
    onReservationsSelected: () -> Unit,
    onNotificationsSelected: () -> Unit,
    onMyComplexSelected: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    isOwner: Boolean = false,
    title: String = "Mejengueros",
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
              title = title,
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
                  buildList {
                    add(
                        MejenguerosBottomNavigationItem(
                            label = "Buscar",
                            selected = selectedRoute == AuthenticatedTopLevelRoute.Search,
                            onClick = onSearchSelected,
                        )
                    )
                    add(
                        MejenguerosBottomNavigationItem(
                            label = "Reservas",
                            selected = selectedRoute == AuthenticatedTopLevelRoute.Reservations,
                            onClick = onReservationsSelected,
                        )
                    )
                    add(
                        MejenguerosBottomNavigationItem(
                            label = "Notificaciones",
                            selected = selectedRoute == AuthenticatedTopLevelRoute.Notifications,
                            onClick = onNotificationsSelected,
                        )
                    )
                    if (isOwner) {
                      add(
                          MejenguerosBottomNavigationItem(
                              label = "Mi complejo",
                              selected = selectedRoute == AuthenticatedTopLevelRoute.MyComplex,
                              onClick = onMyComplexSelected,
                          )
                      )
                    }
                  }
          )
        },
        content = content,
    )

    if (overlayVisible) {
      overlayContent()
    }

    if (showSignOutConfirmation) {
      MejenguerosConfirmationDialog(
          title = "¿Cerrar sesión?",
          message = "Tendrás que iniciar sesión de nuevo para continuar.",
          confirmText = "Cerrar sesión",
          dismissText = "Cancelar",
          onConfirm = {
            showSignOutConfirmation = false
            onSignOut()
          },
          onDismissRequest = { showSignOutConfirmation = false },
      )
    }
  }
}

enum class AuthenticatedTopLevelRoute {
  Search,
  Reservations,
  Notifications,
  MyComplex,
}
