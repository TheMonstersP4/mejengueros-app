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
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosOtpCodeField
import io.github.themonstersp4.mejengueros.ui.components.clearFocusOnTap

@Composable
fun VerifyAccountScreen(
    state: AuthUiState,
    modifier: Modifier = Modifier,
    onBackToRegister: () -> Unit,
    onBackToLogin: () -> Unit,
    onConfirmRegistration: (code: String) -> Unit,
    onResendRegistrationCode: () -> Unit,
) {
  var code by rememberSaveable { mutableStateOf("") }
  val sanitizedCode = code.filter { it.isDigit() }.take(6)
  val formEnabled = !state.isLoading

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
        TextButton(onClick = onBackToRegister, enabled = formEnabled) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
          )
          Text("Volver")
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          MejenguerosAuthHeadingText(
              text = "Verificar cuenta",
              color = MaterialTheme.colorScheme.onSurface,
          )
          MejenguerosAuthTaglineText(
              text = "Ingresa el código de seis dígitos enviado a tu correo.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        MejenguerosFormStack(verticalSpacing = 16.dp) {
          MejenguerosOtpCodeField(
              code = sanitizedCode,
              onCodeChange = { code = it },
              enabled = formEnabled,
              supportingText = "Código enviado a ${state.emailInput.ifBlank { "tu correo" }}.",
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
            text = if (state.isLoading) "Verificando..." else "Verificar cuenta",
            onClick = { onConfirmRegistration(sanitizedCode) },
            enabled = formEnabled && sanitizedCode.length == 6,
        )
        MejenguerosFullWidthOutlinedButton(
            text = "Reenviar código",
            onClick = onResendRegistrationCode,
            enabled = formEnabled,
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
