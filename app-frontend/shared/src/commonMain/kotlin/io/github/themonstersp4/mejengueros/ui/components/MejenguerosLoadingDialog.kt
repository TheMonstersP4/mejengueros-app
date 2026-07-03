package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun MejenguerosLoadingDialog(
    visible: Boolean,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    cancelText: String = "Cancelar",
    dialogTestTag: String = "mejengueros_loading_dialog",
    indicatorTestTag: String = "mejengueros_loading_dialog_indicator",
    cancelButtonTestTag: String = "mejengueros_loading_dialog_cancel",
) {
  if (!visible) {
    return
  }

  Dialog(
      onDismissRequest = { onCancel?.invoke() },
      properties =
          DialogProperties(
              dismissOnBackPress = onCancel != null,
              dismissOnClickOutside = false,
          ),
  ) {
    Surface(
        modifier = modifier.testTag(dialogTestTag),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
      Column(
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        CircularProgressIndicator(modifier = Modifier.testTag(indicatorTestTag))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        MejenguerosSupportingText(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onCancel != null) {
          TextButton(onClick = onCancel, modifier = Modifier.testTag(cancelButtonTestTag)) {
            Text(text = cancelText)
          }
        }
      }
    }
  }
}
