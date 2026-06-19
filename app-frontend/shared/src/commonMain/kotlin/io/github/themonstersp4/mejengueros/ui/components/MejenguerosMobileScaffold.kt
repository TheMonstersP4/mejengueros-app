package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MejenguerosMobileScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
  Scaffold(
      modifier = modifier,
      topBar = topBar,
      bottomBar = bottomBar,
      content = content,
  )
}
