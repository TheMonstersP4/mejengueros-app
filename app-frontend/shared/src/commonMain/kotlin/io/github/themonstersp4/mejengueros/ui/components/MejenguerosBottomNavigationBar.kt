package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class MejenguerosBottomNavigationItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit = {},
)

@Composable
fun MejenguerosBottomNavigationBar(
    items: List<MejenguerosBottomNavigationItem>,
    modifier: Modifier = Modifier,
) {
  NavigationBar(
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    items.forEach { item ->
      NavigationBarItem(
          selected = item.selected,
          onClick = item.onClick,
          label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
          icon = item.icon,
          colors =
              NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.primary,
                  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
      )
    }
  }
}
