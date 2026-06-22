package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
    leadingContent: (@Composable () -> Unit)? = null,
) {
  Button(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.height(48.dp),
      shape = CircleShape,
      contentPadding = PaddingValues(horizontal = 28.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
  ) {
    ButtonLeadingContent(leadingContent = leadingContent)
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
  }
}

@Composable
fun MejenguerosFullWidthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
) {
  MejenguerosPrimaryButton(
      text = text,
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.fillMaxWidth(),
      leadingContent = leadingContent,
  )
}

@Composable
fun MejenguerosOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
) {
  OutlinedButton(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.height(48.dp),
      shape = CircleShape,
      contentPadding = PaddingValues(horizontal = 28.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
      colors =
          ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.primary,
          ),
  ) {
    ButtonLeadingContent(leadingContent = leadingContent)
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
  }
}

@Composable
fun MejenguerosFullWidthOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
) {
  MejenguerosOutlinedButton(
      text = text,
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.fillMaxWidth(),
      leadingContent = leadingContent,
  )
}

@Composable
private fun ButtonLeadingContent(leadingContent: (@Composable () -> Unit)?) {
  if (leadingContent == null) {
    return
  }

  leadingContent()
  Spacer(modifier = Modifier.width(ButtonLeadingContentSpacing))
}

private val ButtonLeadingContentSpacing = 12.dp
