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
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosEmailField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFormStack
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSupportingText
import io.github.themonstersp4.mejengueros.ui.components.clearFocusOnTap

@Composable
fun ForgotPasswordScreen(
    state: AuthUiState,
    modifier: Modifier = Modifier,
    onBackToLogin: () -> Unit,
    onSendCode: (email: String) -> Unit,
) {
  var email by rememberSaveable(state.emailInput) { mutableStateOf(state.emailInput) }
  val formEnabled = !state.isLoading
  val hasEmail = email.isNotBlank()
  val primaryLabel =
      when {
        state.isLoading -> "Enviando..."
        state.errorMessage != null -> "Reintentar"
        else -> "Enviar código"
      }

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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          MejenguerosAuthHeadingText(
              text = "Recuperar acceso",
              color = MaterialTheme.colorScheme.onSurface,
          )
          MejenguerosAuthTaglineText(
              text = "Ingresa tu correo para enviarte un código de recuperación.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        MejenguerosFormStack(verticalSpacing = 16.dp) {
          MejenguerosEmailField(
              value = email,
              onValueChange = { email = it },
              enabled = formEnabled,
          )
          MejenguerosSupportingText(
              text = "Si la cuenta existe, enviaremos un código para cambiar la contraseña.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            text = primaryLabel,
            onClick = { onSendCode(email) },
            enabled = formEnabled && hasEmail,
        )
      }

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
