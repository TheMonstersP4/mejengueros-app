package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MejenguerosConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    onDismissRequest: () -> Unit = {},
) {
  AlertDialog(
      modifier = modifier.testTag("mejengueros_confirmation_dialog"),
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, alignment = Alignment.End),
        ) {
          dismissText?.let {
            MejenguerosOutlinedButton(
                text = it,
                onClick = onDismissRequest,
                modifier = Modifier.testTag("mejengueros_confirmation_dialog_dismiss"),
            )
          }
          MejenguerosPrimaryButton(
              text = confirmText,
              onClick = onConfirm,
              modifier = Modifier.testTag("mejengueros_confirmation_dialog_confirm"),
          )
        }
      },
      dismissButton = {},
  )
}
