package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MejenguerosInlineLoadingState(
    text: String,
    modifier: Modifier = Modifier,
    containerTestTag: String? = null,
    indicatorTestTag: String? = null,
) {
  val taggedModifier =
      if (containerTestTag != null) {
        modifier.testTag(containerTestTag)
      } else {
        modifier
      }

  Column(
      modifier = taggedModifier,
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    CircularProgressIndicator(
        modifier =
            if (indicatorTestTag != null) {
              Modifier.size(20.dp).testTag(indicatorTestTag)
            } else {
              Modifier.size(20.dp)
            },
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 2.dp,
    )
    MejenguerosSupportingText(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
  }
}
