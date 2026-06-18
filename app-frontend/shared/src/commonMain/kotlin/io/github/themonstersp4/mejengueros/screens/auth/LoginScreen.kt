package io.github.themonstersp4.mejengueros.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosEmailField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosPasswordField
import io.github.themonstersp4.mejengueros.ui.components.clearFocusOnTap

@Composable
fun LoginScreen(
    state: AuthUiState,
    modifier: Modifier = Modifier,
    onEmailSignIn: (email: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onMicrosoftSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
) {
  var email by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  val canSubmitEmailPassword = false

  Surface(
      modifier = modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.surface,
  ) {
    Column(
        modifier =
            Modifier.fillMaxSize().clearFocusOnTap().padding(horizontal = 20.dp, vertical = 32.dp),
    ) {
      Column(
          modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
      ) {
        LoginBrandHeader(modifier = Modifier.padding(top = 32.dp, bottom = 32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          MejenguerosEmailField(
              value = email,
              onValueChange = { email = it },
              enabled = !state.isLoading,
          )
          MejenguerosPasswordField(
              value = password,
              onValueChange = { password = it },
              enabled = !state.isLoading,
          )
          Text(
              text = "Por ahora inicia sesión con Google o Microsoft.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          TextButton(
              onClick = onForgotPassword,
              enabled = !state.isLoading,
              modifier = Modifier.align(Alignment.End),
          ) {
            Text(
                text = "¿Olvidaste tu contraseña?",
                style = MaterialTheme.typography.labelLarge,
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
        MejenguerosFullWidthPrimaryButton(
            text = "Correo y contraseña no disponible aún",
            onClick = { onEmailSignIn(email, password) },
            enabled = canSubmitEmailPassword,
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "O continuá con",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          MejenguerosFullWidthOutlinedButton(
              text = "Continuar con Google",
              onClick = onGoogleSignIn,
              enabled = !state.isLoading,
          )
          MejenguerosFullWidthOutlinedButton(
              text = "Continuar con Microsoft",
              onClick = onMicrosoftSignIn,
              enabled = !state.isLoading,
          )
        }

        state.errorMessage?.let { message ->
          Spacer(modifier = Modifier.height(16.dp))
          Text(
              text = message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      TextButton(
          onClick = onRegister,
          enabled = !state.isLoading,
          modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text(
            text = "¿No tienes cuenta? Regístrate",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
      }
    }
  }
}

@Composable
private fun LoginBrandHeader(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
        modifier =
            Modifier.size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(22.dp),
                ),
        contentAlignment = Alignment.Center,
    ) {
      Text(
          text = "M",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onPrimary,
          fontWeight = FontWeight.SemiBold,
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Mejengueros",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Encontrá cancha y armá la mejenga.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
  }
}
