package io.github.themonstersp4.mejengueros.screens.complexes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFormStack
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSupportingText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTextField

@Composable
fun CreateComplexScreen(
    state: CreateComplexUiState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onComplexNameChange: (String) -> Unit,
    onComplexAddressChange: (String) -> Unit,
    onFirstCourtNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(contentPadding)
              .padding(horizontal = 20.dp, vertical = 24.dp)
              .verticalScroll(rememberScrollState())
              .testTag("create_complex_root"),
      verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
          text = "Crear complejo",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
      )
      MejenguerosSupportingText(
          text =
              "Registrá el nombre del complejo, su dirección y la primera cancha en un solo paso.",
      )
    }

    MejenguerosFormStack(verticalSpacing = 14.dp) {
      MejenguerosTextField(
          value = state.complexName,
          onValueChange = onComplexNameChange,
          label = "Nombre del complejo",
          enabled = !state.isSubmitting,
      )
      MejenguerosTextField(
          value = state.complexAddress,
          onValueChange = onComplexAddressChange,
          label = "Dirección",
          enabled = !state.isSubmitting,
      )
      MejenguerosTextField(
          value = state.firstCourtName,
          onValueChange = onFirstCourtNameChange,
          label = "Nombre de la primera cancha",
          enabled = !state.isSubmitting,
      )
    }

    MejenguerosSupportingText(
        text =
            "Si tu usuario todavía no tiene el rol OWNER local, el sistema mostrará el bloqueo para que puedas pedir la provisión demo.",
    )

    state.errorMessage?.let { message ->
      MejenguerosErrorText(
          text = message,
          textAlign = TextAlign.Start,
          modifier = Modifier.testTag("create_complex_error"),
      )
    }

    state.successMessage?.let { message ->
      Card(modifier = Modifier.testTag("create_complex_success")) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
              text = message,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
          )
          state.createdComplex?.let { createdComplex ->
            MejenguerosSupportingText(
                text =
                    "${createdComplex.complexName} · ${createdComplex.complexAddress}\nPrimera cancha: ${createdComplex.firstCourtName}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    MejenguerosFullWidthPrimaryButton(
        text = if (state.isSubmitting) "Creando..." else "Crear complejo",
        onClick = onSubmit,
        enabled = state.canSubmit,
        modifier = Modifier.testTag("create_complex_submit_button"),
    )
  }
}
