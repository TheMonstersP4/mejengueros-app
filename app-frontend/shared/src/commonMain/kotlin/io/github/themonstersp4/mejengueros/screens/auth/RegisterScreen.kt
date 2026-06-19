package io.github.themonstersp4.mejengueros.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosEmailField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFormStack
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosPasswordField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTextField
import io.github.themonstersp4.mejengueros.ui.components.clearFocusOnTap

@Composable
fun RegisterScreen(
    state: AuthUiState,
    modifier: Modifier = Modifier,
    onBackToLogin: () -> Unit,
    onOpenVerification: () -> Unit,
) {
  var fullName by rememberSaveable { mutableStateOf("") }
  var email by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }
  var pendingMessage by rememberSaveable { mutableStateOf<String?>(null) }
  val formEnabled = !state.isLoading

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column(modifier = Modifier.fillMaxSize().clearFocusOnTap().padding(20.dp)) {
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
          Text(
              text = "Crear cuenta",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
              text = "Este formulario prepara el flujo visual de registro de semana 10.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        MejenguerosFormStack {
          MejenguerosTextField(
              value = fullName,
              onValueChange = { fullName = it },
              label = "Nombre completo",
              enabled = formEnabled,
          )
          MejenguerosEmailField(
              value = email,
              onValueChange = { email = it },
              enabled = formEnabled,
          )
          MejenguerosPasswordField(
              value = password,
              onValueChange = { password = it },
              enabled = formEnabled,
              supportingText = "Mínimo 8 caracteres cuando el registro esté conectado.",
          )
          MejenguerosPasswordField(
              value = confirmPassword,
              onValueChange = { confirmPassword = it },
              label = "Confirmar contraseña",
              enabled = formEnabled,
          )
        }

        Text(
            text =
                "Al registrarte aceptarás los términos y políticas cuando el flujo productivo esté disponible.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        pendingMessage?.let { message ->
          Text(
              text = message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth(),
          )
        }

        MejenguerosFullWidthPrimaryButton(
            text = "Registro manual pendiente",
            onClick = { pendingMessage = "El registro manual aún no está conectado al backend." },
            enabled = formEnabled,
        )
        MejenguerosFullWidthOutlinedButton(
            text = "Ver pantalla de verificación",
            onClick = onOpenVerification,
            enabled = formEnabled,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      TextButton(
          onClick = onBackToLogin,
          enabled = formEnabled,
          modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text(
            text = "¿Ya tienes cuenta? Inicia sesión",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
      }
    }
  }
}
