package io.github.themonstersp4.mejengueros.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState
import io.github.themonstersp4.mejengueros.ui.components.GoogleProviderIcon
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosAuthHeadingText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosAuthTaglineText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosEmailField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosPasswordField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSupportingText
import io.github.themonstersp4.mejengueros.ui.components.MicrosoftProviderIcon
import io.github.themonstersp4.mejengueros.ui.components.clearFocusOnTap
import kotlin.math.min

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
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
) {
  var email by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
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
        LoginPitchHero(modifier = Modifier.padding(top = 8.dp))
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
}

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
private fun LoginPitchHero(modifier: Modifier = Modifier) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      shape = RoundedCornerShape(28.dp),
      border =
          BorderStroke(
              width = 1.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f),
          ),
  ) {
    BoxWithConstraints(
        modifier =
            Modifier.fillMaxWidth().height(184.dp).padding(horizontal = 18.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
      val fieldHeight = maxHeight - 24.dp
      GoalPitchField(
          modifier = Modifier.fillMaxWidth().height(fieldHeight),
          lineThickness = 2.dp,
      )
      GoalPitchBall(modifier = Modifier.size(56.dp))
    }
  }
}

@Composable
private fun GoalPitchField(
    modifier: Modifier = Modifier,
    lineThickness: Dp,
) {
  val lineColor = MaterialTheme.colorScheme.primary
  val haloColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

  Box(
      modifier =
          modifier
              .clip(RoundedCornerShape(24.dp))
              .background(MaterialTheme.colorScheme.surfaceContainerHighest),
  ) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .border(
                    width = lineThickness,
                    color = lineColor.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(24.dp),
                ),
    )
    Box(
        modifier =
            Modifier.align(Alignment.Center)
                .fillMaxWidth()
                .height(lineThickness)
                .background(lineColor.copy(alpha = 0.72f)),
    )
    Box(
        modifier =
            Modifier.align(Alignment.Center)
                .size(72.dp)
                .border(
                    width = lineThickness,
                    color = lineColor.copy(alpha = 0.82f),
                    shape = CircleShape,
                ),
    )
    Box(
        modifier =
            Modifier.align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .fillMaxWidth(0.18f)
                .height(54.dp)
                .border(
                    width = lineThickness,
                    color = haloColor,
                    shape = RoundedCornerShape(16.dp),
                ),
    )
    Box(
        modifier =
            Modifier.align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .fillMaxWidth(0.18f)
                .height(54.dp)
                .border(
                    width = lineThickness,
                    color = haloColor,
                    shape = RoundedCornerShape(16.dp),
                ),
    )
    Box(
        modifier =
            Modifier.align(Alignment.CenterStart)
                .padding(start = 28.dp)
                .size(8.dp)
                .background(lineColor, CircleShape),
    )
    Box(
        modifier =
            Modifier.align(Alignment.CenterEnd)
                .padding(end = 28.dp)
                .size(8.dp)
                .background(lineColor, CircleShape),
    )
  }
}

@Composable
private fun GoalPitchBall(modifier: Modifier = Modifier) {
  val fillColor = MaterialTheme.colorScheme.primary
  val seamColor = MaterialTheme.colorScheme.onPrimary

  Surface(
      modifier = modifier,
      color = fillColor,
      shape = CircleShape,
      shadowElevation = 0.dp,
  ) {
    Box(contentAlignment = Alignment.Center) {
      androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(11.dp)) {
        val center = center
        val radius = min(size.width, size.height) / 2f
        val seamStroke = radius * 0.16f

        drawCircle(
            color = seamColor.copy(alpha = 0.92f),
            radius = radius * 0.36f,
            center = center,
            style = Stroke(width = seamStroke),
        )
        drawLine(
            color = seamColor.copy(alpha = 0.92f),
            start = Offset(center.x - radius * 0.72f, center.y),
            end = Offset(center.x + radius * 0.72f, center.y),
            strokeWidth = seamStroke,
        )
        drawLine(
            color = seamColor.copy(alpha = 0.92f),
            start = Offset(center.x, center.y - radius * 0.72f),
            end = Offset(center.x, center.y + radius * 0.72f),
            strokeWidth = seamStroke,
        )
      }
    }
  }
}
