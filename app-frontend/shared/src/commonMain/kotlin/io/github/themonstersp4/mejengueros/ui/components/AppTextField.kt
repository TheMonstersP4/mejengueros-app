package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun MejenguerosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier.fillMaxWidth(),
      enabled = enabled,
      readOnly = readOnly,
      singleLine = singleLine,
      minLines = minLines,
      maxLines = maxLines,
      isError = isError,
      label = { Text(label) },
      supportingText = supportingText?.let { message -> ({ Text(message) }) },
      keyboardOptions = keyboardOptions,
      visualTransformation = visualTransformation,
      trailingIcon = trailingIcon,
      shape = MaterialTheme.shapes.medium,
      colors = mejenguerosTextFieldColors(),
  )
}

@Composable
private fun mejenguerosTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorLabelColor = MaterialTheme.colorScheme.error,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        cursorColor = MaterialTheme.colorScheme.primary,
        errorCursorColor = MaterialTheme.colorScheme.error,
        focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorSupportingTextColor = MaterialTheme.colorScheme.error,
    )

@Composable
fun MejenguerosTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    minLines: Int = 3,
    maxLines: Int = 5,
) {
  MejenguerosTextField(
      value = value,
      onValueChange = onValueChange,
      label = label,
      modifier = modifier,
      enabled = enabled,
      isError = isError,
      supportingText = supportingText,
      singleLine = false,
      minLines = minLines,
      maxLines = maxLines,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MejenguerosSelectField(
    value: String,
    label: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }

  ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { if (enabled) expanded = it },
      modifier = modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier =
            Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        enabled = enabled,
        readOnly = true,
        singleLine = true,
        isError = isError,
        label = { Text(label) },
        supportingText = supportingText?.let { message -> ({ Text(message) }) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        shape = MaterialTheme.shapes.medium,
        colors = mejenguerosTextFieldColors(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
            text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
            onClick = {
              onOptionSelected(option)
              expanded = false
            },
            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
        )
      }
    }
  }
}

@Composable
fun MejenguerosEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
) {
  MejenguerosTextField(
      value = value,
      onValueChange = onValueChange,
      label = "Correo electrónico",
      modifier = modifier,
      enabled = enabled,
      isError = isError,
      supportingText = supportingText,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
  )
}

fun Modifier.clearFocusOnTap(focusManager: FocusManager? = null): Modifier = composed {
  val currentFocusManager = focusManager ?: LocalFocusManager.current

  pointerInput(currentFocusManager) {
    detectTapGestures(onTap = { currentFocusManager.clearFocus() })
  }
}

@Composable
fun MejenguerosPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Contraseña",
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
) {
  var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
  val visibilityDescription = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
  val visualTransformation =
      remember(isPasswordVisible) {
        if (isPasswordVisible) {
          VisualTransformation.None
        } else {
          PasswordVisualTransformation()
        }
      }

  MejenguerosTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier,
      label = label,
      enabled = enabled,
      isError = isError,
      supportingText = supportingText,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
      visualTransformation = visualTransformation,
      trailingIcon = {
        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }, enabled = enabled) {
          VisibilityIcon(
              visible = isPasswordVisible,
              contentDescription = visibilityDescription,
          )
        }
      },
  )
}
