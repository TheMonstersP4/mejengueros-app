package io.github.themonstersp4.mejengueros.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFormStack
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosOtpCodeField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTextField
import io.github.themonstersp4.mejengueros.ui.components.clearFocusOnTap

@Composable
fun VerifyAccountScreen(
    modifier: Modifier = Modifier,
    onBackToRegister: () -> Unit,
    onBackToLogin: () -> Unit,
) {
  var code by rememberSaveable { mutableStateOf("") }
  var pendingMessage by rememberSaveable { mutableStateOf<String?>(null) }
  val sanitizedCode = code.filter { it.isDigit() }.take(6)

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
    Column(modifier = Modifier.fillMaxSize().clearFocusOnTap().padding(20.dp)) {
      Column(
          modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        TextButton(onClick = onBackToRegister) { Text("‹ Volver") }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
              text = "Verificar cuenta",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
              text =
                  "Ingresa el código de seis dígitos enviado a tu correo cuando el flujo esté disponible.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        MejenguerosFormStack(verticalSpacing = 16.dp) {
          MejenguerosOtpCodeField(
              code = sanitizedCode,
              supportingText = "Componente visual preparado para el código de verificación.",
          )
          MejenguerosTextField(
              value = sanitizedCode,
              onValueChange = { code = it.filter(Char::isDigit).take(6) },
              label = "Código de verificación",
              supportingText = "La verificación real se conectará con el backend más adelante.",
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          )
        }

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
            text = "Verificación pendiente",
            onClick = { pendingMessage = "La verificación de cuenta aún no está conectada." },
        )
        MejenguerosFullWidthOutlinedButton(
            text = "Reenviar código no disponible aún",
            onClick = { pendingMessage = "El reenvío de código todavía no está disponible." },
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      TextButton(
          onClick = onBackToLogin,
          modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text("Volver al inicio de sesión")
      }
    }
  }
}
