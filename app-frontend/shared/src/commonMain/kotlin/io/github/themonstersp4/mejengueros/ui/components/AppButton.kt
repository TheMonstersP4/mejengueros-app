package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MejenguerosPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  Button(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.height(44.dp),
      shape = CircleShape,
      contentPadding = PaddingValues(horizontal = 22.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
  ) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Composable
fun MejenguerosFullWidthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  MejenguerosPrimaryButton(
      text = text,
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.fillMaxWidth(),
  )
}

@Composable
fun MejenguerosOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  OutlinedButton(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.height(44.dp),
      shape = CircleShape,
      contentPadding = PaddingValues(horizontal = 22.dp),
      colors =
          ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.primary,
          ),
  ) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Composable
fun MejenguerosFullWidthOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  MejenguerosOutlinedButton(
      text = text,
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.fillMaxWidth(),
  )
}
