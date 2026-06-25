package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.runtime.Composable

val DefaultMejenguerosLocationPickerCenter =
    SelectedLocation(latitude = 9.93510, longitude = -84.09110)

@Composable
fun MejenguerosLocationPickerOverlay(
    state: MejenguerosLocationPickerState,
    actions: MejenguerosLocationPickerActions,
) {
  MejenguerosLocationPickerScreen(
      state = state,
      actions = actions,
      mapContent = { scope -> MejenguerosOpenFreeMapLocationPickerMap(scope = scope) },
  )
}
