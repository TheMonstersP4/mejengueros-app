package io.github.themonstersp4.mejengueros.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.presentation.auth.AuthUiState

@Composable
fun LoginScreen(
    state: AuthUiState,
    onGoogleSignIn: () -> Unit,
    onMicrosoftSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Surface(
      modifier = modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.background,
  ) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
      Text(
          text = "Mejengueros",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
          text = "Choose how you want to continue.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(24.dp))
      Button(
          onClick = onGoogleSignIn,
          enabled = !state.isLoading,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text(if (state.isLoading) "Opening browser..." else "Continue with Google")
      }
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedButton(
          onClick = onMicrosoftSignIn,
          enabled = !state.isLoading,
          modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Continue with Microsoft")
      }
      state.errorMessage?.let { message ->
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
      }
    }
  }
}
