package example.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable sealed interface AppRoute : NavKey
@Serializable data object LaunchList : AppRoute
@Serializable data object Profile : AppRoute
@Serializable data class LaunchDetail(val flightNumber: Int) : AppRoute

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack<AppRoute>(LaunchList)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<LaunchList> {
                LaunchListRoute(
                    onLaunchClick = { launch -> backStack.add(LaunchDetail(launch.flightNumber)) },
                )
            }
            entry<LaunchDetail> { key ->
                LaunchDetailRoute(flightNumber = key.flightNumber)
            }
            entry<Profile> {
                ProfileRoute()
            }
        },
    )
}
