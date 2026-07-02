package io.github.themonstersp4.mejengueros.screens.complexes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.presentation.complexes.AddCourtUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListGroup
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListItem
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListItemStyle
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListItemText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPill
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPillStyle
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTextField

class AddCourtScreenActions(
    val onRetryServices: () -> Unit,
    val onCourtNameChange: (String) -> Unit,
    val onToggleService: (String) -> Unit,
    val onPickCourtImage: () -> Unit,
    val onClearCourtImage: () -> Unit,
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

    AddCourtImageSection(
        isPickerAvailable = state.isCourtImagePickerAvailable,
        selectedCourtImage = state.selectedCourtImage,
        onPickCourtImage = actions.onPickCourtImage,
        onClearCourtImage = actions.onClearCourtImage,
        enabled = !state.isSubmitting,
    )

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
private fun AddCourtImageSection(
    isPickerAvailable: Boolean,
    selectedCourtImage: LocalCourtImage?,
    onPickCourtImage: () -> Unit,
    onClearCourtImage: () -> Unit,
    enabled: Boolean,
) {
  if (!isPickerAvailable && selectedCourtImage == null) {
    return
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
        text = "IMAGEN DE LA CANCHA",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = "Opcional. Podés agregar una imagen ahora o dejarla para más adelante.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )

    selectedCourtImage?.let { courtImage ->
      Card(modifier = Modifier.fillMaxWidth().testTag("add_court_image_preview")) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            AsyncImage(
                model = courtImage.previewUrl,
                contentDescription = "Vista previa de la imagen de la cancha",
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )
          }
          Text(text = courtImage.fileName, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }

    MejenguerosFullWidthOutlinedButton(
        text = if (selectedCourtImage == null) "Seleccionar imagen" else "Cambiar imagen",
        onClick = onPickCourtImage,
        enabled = enabled && isPickerAvailable,
        modifier = Modifier.testTag("add_court_pick_image_button"),
    )

    if (selectedCourtImage != null) {
      MejenguerosFullWidthOutlinedButton(
          text = "Quitar imagen",
          onClick = onClearCourtImage,
          enabled = enabled,
          modifier = Modifier.testTag("add_court_clear_image_button"),
      )
    }
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
            text =
                MejenguerosListItemText(
                    title = service.name,
                    supportingText = if (selected) "Seleccionado" else "Tocá para seleccionar",
                ),
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
            style = MejenguerosListItemStyle(showDivider = index < services.lastIndex),
        )
      }
    }
  }
}
