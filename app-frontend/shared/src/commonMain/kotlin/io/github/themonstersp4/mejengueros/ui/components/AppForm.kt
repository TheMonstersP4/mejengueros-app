package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MejenguerosFormStack(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(verticalSpacing),
      content = content,
  )
}
