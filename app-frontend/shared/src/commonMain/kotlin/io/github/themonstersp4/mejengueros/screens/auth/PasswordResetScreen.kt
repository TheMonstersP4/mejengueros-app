package io.github.themonstersp4.mejengueros.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosAuthHeadingText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosAuthTaglineText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFormStack
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosOtpCodeField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosPasswordField
import io.github.themonstersp4.mejengueros.ui.components.clearFocusOnTap

@Composable
fun PasswordResetScreen(
    state: AuthUiState,
    modifier: Modifier = Modifier,
    onBackToLogin: () -> Unit,
    onConfirmPasswordReset: (code: String, newPassword: String) -> Unit,
) {
  var code by rememberSaveable { mutableStateOf("") }
  var newPassword by rememberSaveable { mutableStateOf("") }
  val sanitizedCode = code.filter { it.isDigit() }.take(6)
  val formEnabled = !state.isLoading
  val passwordWasUpdated = state.successMessage != null

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .clearFocusOnTap()
                .safeDrawingPadding()
                .imePadding()
                .padding(20.dp)
    ) {
      Column(
          modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        TextButton(onClick = onBackToLogin, enabled = formEnabled) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
          )
          Text("Volver")
        }

        if (passwordWasUpdated) {
          PasswordResetSuccessContent(
              message = state.successMessage.orEmpty(),
              onBackToLogin = onBackToLogin,
          )
        } else {
          PasswordResetFormContent(
              state = state,
              code = sanitizedCode,
              newPassword = newPassword,
              formEnabled = formEnabled,
              onCodeChange = { code = it },
              onPasswordChange = { newPassword = it },
              onConfirmPasswordReset = onConfirmPasswordReset,
          )
        }
      }

      if (!passwordWasUpdated) {
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = onBackToLogin,
            enabled = formEnabled,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
          Text("Volver al inicio de sesión")
        }
      }
    }
  }
}

@Composable
private fun PasswordResetFormContent(
    state: AuthUiState,
    code: String,
    newPassword: String,
    formEnabled: Boolean,
    onCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordReset: (code: String, newPassword: String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    MejenguerosAuthHeadingText(
        text = "Recuperar acceso",
        color = MaterialTheme.colorScheme.onSurface,
    )
    MejenguerosAuthTaglineText(
        text = "Ingresa el código enviado a tu correo y define una nueva contraseña.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }

  MejenguerosFormStack(verticalSpacing = 16.dp) {
    MejenguerosOtpCodeField(
        code = code,
        onCodeChange = onCodeChange,
        label = "Código de recuperación",
        supportingText = "Código enviado a ${state.emailInput.ifBlank { "tu correo" }}.",
        enabled = formEnabled,
    )
    MejenguerosPasswordField(
        value = newPassword,
        onValueChange = onPasswordChange,
        label = "Nueva contraseña",
        supportingText = PasswordPolicySupportingText,
        enabled = formEnabled,
    )
  }

  state.errorMessage?.let { message ->
    MejenguerosErrorText(
        text = message,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
  }

  MejenguerosFullWidthPrimaryButton(
      text = if (state.isLoading) "Actualizando..." else "Actualizar contraseña",
      onClick = { onConfirmPasswordReset(code, newPassword) },
      enabled = formEnabled && code.length == 6 && newPassword.isNotBlank(),
  )
}

@Composable
private fun PasswordResetSuccessContent(
    message: String,
    onBackToLogin: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    MejenguerosAuthHeadingText(
        text = "Contraseña actualizada",
        color = MaterialTheme.colorScheme.onSurface,
    )
    MejenguerosAuthTaglineText(
        text = message.ifBlank { "Ya puedes iniciar sesión con tu nueva contraseña." },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }

  MejenguerosFullWidthPrimaryButton(
      text = "Ir a iniciar sesión",
      onClick = onBackToLogin,
  )
}
