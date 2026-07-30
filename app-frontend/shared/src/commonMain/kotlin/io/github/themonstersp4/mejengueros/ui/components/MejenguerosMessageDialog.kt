package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign

@Composable
fun MejenguerosMessageDialog(
    visible: Boolean,
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    actionText: String = "Entendido",
) {
  if (!visible) return

  AlertDialog(
      modifier = modifier.testTag("mejengueros_message_dialog"),
      onDismissRequest = onDismissRequest,
      title = {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
      },
      text = {
        MejenguerosSupportingText(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
      },
      confirmButton = {
        MejenguerosPrimaryButton(
            text = actionText,
            onClick = onDismissRequest,
            modifier = Modifier.testTag("mejengueros_message_dialog_action"),
        )
      },
      dismissButton = {},
  )
}
