package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MejenguerosBottomActionBar(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues =
        PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Column {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Column(
          modifier = Modifier.fillMaxWidth().padding(contentPadding),
          content = content,
      )
    }
  }
}
