package io.github.themonstersp4.mejengueros.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.AuthProvider
import io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState
import io.github.themonstersp4.mejengueros.ui.components.GoogleProviderIcon
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosAuthHeadingText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosAuthTaglineText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosBrandMark
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosEmailField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLoadingDialog
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosPasswordField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSupportingText
import io.github.themonstersp4.mejengueros.ui.components.MicrosoftProviderIcon
import io.github.themonstersp4.mejengueros.ui.components.clearFocusOnTap

private data class LoginEmailAccessUiModel(
    val enabled: Boolean,
    val supportingText: String,
)

private fun resolveLoginEmailAccessUiModel(
    email: String,
    password: String,
    isLoading: Boolean,
): LoginEmailAccessUiModel {
  val hasCredentials = email.isNotBlank() && password.isNotBlank()

  return if (hasCredentials) {
    LoginEmailAccessUiModel(
        enabled = !isLoading,
        supportingText = "Entrá con tu cuenta de correo o continuá con Google o Microsoft.",
    )
  } else {
    LoginEmailAccessUiModel(
        enabled = false,
        supportingText = "Completá tu correo y contraseña para continuar.",
    )
  }
}

@Composable
fun LoginScreen(
    state: AuthUiState,
    modifier: Modifier = Modifier,
    onEmailSignIn: (email: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onMicrosoftSignIn: () -> Unit,
    onCancelExternalAuth: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    brandMark: @Composable () -> Unit = { MejenguerosBrandMark(modifier = Modifier.width(142.dp)) },
) {
  var email by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  val showExternalAuthProgress =
      state.isExternalAuthInProgress && !state.isAuthenticated && !state.isRestoringSession
  val emailAccessUiModel =
      resolveLoginEmailAccessUiModel(
          email = email,
          password = password,
          isLoading = state.isLoading,
      )

  Surface(
      modifier = modifier.fillMaxSize().clearFocusOnTap().testTag("login_root"),
      color = MaterialTheme.colorScheme.surface,
  ) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
      Column(
          modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        LoginPitchHero(
            brandMark = brandMark,
            modifier = Modifier.padding(top = 8.dp),
        )
        LoginBrandHeader()

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
          MejenguerosSupportingText(
              text = emailAccessUiModel.supportingText,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          TextButton(
              onClick = onForgotPassword,
              enabled = !state.isLoading,
              modifier = Modifier.align(Alignment.End).testTag("login_forgot_password_button"),
          ) {
            Text(
                text = "¿Olvidaste tu contraseña?",
                style = MaterialTheme.typography.labelLarge,
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
        }

        MejenguerosFullWidthPrimaryButton(
            text = if (state.isLoading) "Ingresando..." else "Continuar con correo",
            onClick = { onEmailSignIn(email, password) },
            enabled = emailAccessUiModel.enabled,
            modifier = Modifier.testTag("login_email_cta_button"),
        )

        LoginProviderSection(
            isLoading = state.isLoading,
            onGoogleSignIn = onGoogleSignIn,
            onMicrosoftSignIn = onMicrosoftSignIn,
        )
      }

      TextButton(
          onClick = onRegister,
          enabled = !state.isLoading,
          modifier = Modifier.align(Alignment.CenterHorizontally).testTag("login_register_button"),
      ) {
        Text(
            text = "¿No tienes cuenta? Regístrate",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
      }
    }
  }

  val externalAuthProgressUiModel = resolveExternalAuthProgressUiModel(state.pendingProvider)

  MejenguerosLoadingDialog(
      visible = showExternalAuthProgress,
      title = externalAuthProgressUiModel.title,
      message = externalAuthProgressUiModel.message,
      onCancel = onCancelExternalAuth,
      dialogTestTag = "login_external_auth_dialog",
      indicatorTestTag = "login_external_auth_loading",
      cancelButtonTestTag = "login_external_auth_cancel_button",
  )
}

private fun resolveExternalAuthProgressUiModel(
    provider: AuthProvider?,
): ExternalAuthProgressUiModel =
    when (provider) {
      AuthProvider.Google ->
          ExternalAuthProgressUiModel(
              title = "Completando acceso con Google",
              message = "Estamos validando tu cuenta para entrar a Mejengueros.",
          )
      AuthProvider.Microsoft ->
          ExternalAuthProgressUiModel(
              title = "Completando acceso con Microsoft",
              message = "Estamos validando tu cuenta para entrar a Mejengueros.",
          )
      null ->
          ExternalAuthProgressUiModel(
              title = "Completando inicio de sesión",
              message = "Estamos terminando tu autenticación. Esto puede tardar unos segundos.",
          )
    }

private data class ExternalAuthProgressUiModel(
    val title: String,
    val message: String,
)

@Composable
private fun LoginBrandHeader(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    MejenguerosAuthHeadingText(
        text = "Mejengueros",
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    MejenguerosAuthTaglineText(
        text = "Encontrá cancha y armá la mejenga.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(0.72f),
    )
  }
}

@Composable
private fun LoginProviderSection(
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit,
    onMicrosoftSignIn: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      HorizontalDivider(
          modifier = Modifier.weight(1f),
          color = MaterialTheme.colorScheme.outlineVariant,
      )
      MejenguerosSupportingText(
          text = "O seguí con un proveedor disponible hoy",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      HorizontalDivider(
          modifier = Modifier.weight(1f),
          color = MaterialTheme.colorScheme.outlineVariant,
      )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MejenguerosFullWidthOutlinedButton(
          text = "Google",
          onClick = onGoogleSignIn,
          enabled = !isLoading,
          modifier = Modifier.weight(1f).testTag("login_google_button"),
          leadingContent = { GoogleProviderIcon(modifier = Modifier.size(18.dp)) },
      )
      MejenguerosFullWidthOutlinedButton(
          text = "Outlook",
          onClick = onMicrosoftSignIn,
          enabled = !isLoading,
          modifier = Modifier.weight(1f).testTag("login_microsoft_button"),
          leadingContent = { MicrosoftProviderIcon(modifier = Modifier.size(18.dp)) },
      )
    }
  }
}

@Composable
private fun LoginPitchHero(
    brandMark: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(
      modifier = modifier.fillMaxWidth().height(184.dp),
      contentAlignment = Alignment.Center,
  ) {
    brandMark()
  }
}
