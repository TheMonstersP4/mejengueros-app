package io.github.themonstersp4.mejengueros.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
    // True when an owner is temporarily viewing the app in player (mejenguero) mode.
    viewingAsPlayer: Boolean = false,
    // Called when the owner taps "Modo mejenguero" in the drawer to enter player mode.
    onSwitchToPlayerView: () -> Unit = {},
    // Called when an owner-in-player-mode taps "Mi complejo" in the top bar to return to owner
    // mode.
    onSwitchToOwnerView: () -> Unit = {},
    title: String = "Mejengueros",
    onNavigateBack: (() -> Unit)? = null,
    overlayVisible: Boolean = false,
    overlayContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
  var showSignOutConfirmation by rememberSaveable { mutableStateOf(false) }
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val drawerScope = rememberCoroutineScope()

  val scaffoldModifier =
      Modifier.fillMaxSize()
          .then(if (overlayVisible) Modifier.clearAndSetSemantics {} else Modifier)

  Box(modifier = modifier.fillMaxSize()) {
    if (isOwner && !viewingAsPlayer) {
      ModalNavigationDrawer(
          drawerState = drawerState,
          modifier = Modifier.fillMaxSize(),
          drawerContent = {
            ModalDrawerSheet {
              Spacer(Modifier.height(12.dp))
              NavigationDrawerItem(
                  label = { Text("Mi complejo") },
                  selected = selectedRoute == AuthenticatedTopLevelRoute.MyComplex,
                  onClick = {
                    drawerScope.launch { drawerState.close() }
                    onMyComplexSelected()
                  },
                  modifier = Modifier.padding(horizontal = 12.dp),
              )
              NavigationDrawerItem(
                  label = { Text("Reservas") },
                  selected = selectedRoute == AuthenticatedTopLevelRoute.Reservations,
                  onClick = {
                    drawerScope.launch { drawerState.close() }
                    onReservationsSelected()
                  },
                  modifier = Modifier.padding(horizontal = 12.dp),
              )
              NavigationDrawerItem(
                  label = { Text("Reseñas") },
                  selected = false,
                  onClick = { drawerScope.launch { drawerState.close() } },
                  modifier = Modifier.padding(horizontal = 12.dp),
              )
              HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
              NavigationDrawerItem(
                  label = { Text("Modo mejenguero") },
                  selected = false,
                  onClick = {
                    drawerScope.launch { drawerState.close() }
                    onSwitchToPlayerView()
                  },
                  modifier = Modifier.padding(horizontal = 12.dp),
              )
            }
          },
      ) {
        MejenguerosMobileScaffold(
            modifier = scaffoldModifier,
            topBar = {
              MejenguerosTopAppBar(
                  title = title,
                  navigationIcon = {
                    if (onNavigateBack != null) {
                      IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            modifier = Modifier.size(20.dp),
                        )
                      }
                    } else {
                      IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Abrir menú",
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
            content = content,
        )
      }
    } else {
      MejenguerosMobileScaffold(
          modifier = scaffoldModifier,
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
                  if (isOwner) {
                    IconButton(onClick = onSwitchToOwnerView) {
                      Icon(
                          imageVector = Icons.Filled.Home,
                          contentDescription = "Mi complejo",
                          modifier = Modifier.size(20.dp),
                      )
                    }
                  }
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
                            label = "Buscar",
                            selected = selectedRoute == AuthenticatedTopLevelRoute.Search,
                            onClick = onSearchSelected,
                            icon = {
                              Icon(
                                  imageVector = Icons.Filled.Search,
                                  contentDescription = "Buscar",
                              )
                            },
                        ),
                        MejenguerosBottomNavigationItem(
                            label = "Reservas",
                            selected = selectedRoute == AuthenticatedTopLevelRoute.Reservations,
                            onClick = onReservationsSelected,
                            icon = {
                              Icon(
                                  imageVector = Icons.Filled.DateRange,
                                  contentDescription = "Reservas",
                              )
                            },
                        ),
                        MejenguerosBottomNavigationItem(
                            label = "Notificaciones",
                            selected = selectedRoute == AuthenticatedTopLevelRoute.Notifications,
                            onClick = onNotificationsSelected,
                            icon = {
                              Icon(
                                  imageVector = Icons.Filled.Notifications,
                                  contentDescription = "Notificaciones",
                              )
                            },
                        ),
                    )
            )
          },
          content = content,
      )
    }

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
