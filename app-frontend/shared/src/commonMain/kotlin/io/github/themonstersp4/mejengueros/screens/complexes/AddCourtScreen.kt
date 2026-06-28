package io.github.themonstersp4.mejengueros.screens.complexes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.presentation.complexes.AddCourtUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListGroup
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListItem
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPill
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPillStyle
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTextField

class AddCourtScreenActions(
    val onRetryServices: () -> Unit,
    val onCourtNameChange: (String) -> Unit,
    val onToggleService: (String) -> Unit,
    val onSubmit: () -> Unit,
)

@Composable
fun AddCourtScreen(
    state: AddCourtUiState,
    contentPadding: PaddingValues,
    actions: AddCourtScreenActions,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(contentPadding)
              .padding(horizontal = 20.dp, vertical = 24.dp)
              .verticalScroll(rememberScrollState())
              .testTag("add_court_root"),
      verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
          text = "Agregar cancha",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
          text = "Completá la nueva cancha para ${state.complexName}",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onBackground,
      )
    }

    MejenguerosTextField(
        value = state.courtName,
        onValueChange = actions.onCourtNameChange,
        label = "Nombre de la cancha",
        modifier = Modifier.testTag("add_court_name_field"),
        supportingText =
            if (state.formErrorMessage != null && state.courtName.isBlank()) state.formErrorMessage
            else null,
    )

    when {
      state.isLoadingServices ->
          CircularProgressIndicator(modifier = Modifier.testTag("add_court_loading_services"))
      state.hasCatalogLoadFailure -> {
        MejenguerosErrorText(text = state.errorMessage.orEmpty())
        MejenguerosFullWidthOutlinedButton(text = "Reintentar", onClick = actions.onRetryServices)
      }
      else ->
          CourtServicesSelector(
              services = state.courtServices,
              selectedServiceIds = state.selectedCourtServiceIds,
              onToggleService = actions.onToggleService,
          )
    }

    val submitError = state.errorMessage
    if (submitError != null && !state.hasCatalogLoadFailure && state.courtName.isNotBlank()) {
      MejenguerosErrorText(text = submitError)
    }

    MejenguerosFullWidthPrimaryButton(
        text = if (state.isSubmitting) "Guardando cancha..." else "Guardar cancha",
        onClick = actions.onSubmit,
        enabled = !state.isSubmitting,
        modifier = Modifier.testTag("add_court_submit_button"),
    )
  }
}

@Composable
private fun CourtServicesSelector(
    services: List<ServiceCatalogItem>,
    selectedServiceIds: List<String>,
    onToggleService: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
        text = "SERVICIOS DE CANCHA",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    MejenguerosListGroup {
      services.forEachIndexed { index, service ->
        val selected = selectedServiceIds.contains(service.id)
        MejenguerosListItem(
            title = service.name,
            supportingText = if (selected) "Seleccionado" else "Tocá para seleccionar",
            modifier = Modifier.testTag("add_court_service_${service.id}"),
            trailing = {
              if (selected) {
                MejenguerosStatusPill(
                    text = "Seleccionado",
                    style = MejenguerosStatusPillStyle.Primary,
                )
              }
            },
            onClick = { onToggleService(service.id) },
            showDivider = index < services.lastIndex,
        )
      }
    }
  }
}
