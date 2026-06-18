package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.dp

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
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier.fillMaxWidth(),
      enabled = enabled,
      singleLine = singleLine,
      isError = isError,
      label = { Text(label) },
      supportingText = supportingText?.let { message -> ({ Text(message) }) },
      keyboardOptions = keyboardOptions,
      visualTransformation = visualTransformation,
      trailingIcon = trailingIcon,
      shape = RoundedCornerShape(8.dp),
      colors =
          OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              focusedLabelColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
              errorBorderColor = MaterialTheme.colorScheme.error,
              errorLabelColor = MaterialTheme.colorScheme.error,
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              disabledContainerColor = MaterialTheme.colorScheme.surface,
          ),
  )
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
