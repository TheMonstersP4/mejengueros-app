package io.github.themonstersp4.mejengueros.screens.complexes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.themonstersp4.mejengueros.domain.model.Canton
import io.github.themonstersp4.mejengueros.domain.model.LocalCourtImage
import io.github.themonstersp4.mejengueros.domain.model.Province
import io.github.themonstersp4.mejengueros.domain.model.ServiceCatalogItem
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexStep
import io.github.themonstersp4.mejengueros.presentation.complexes.CreateComplexUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosConfirmationDialog
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFormStack
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosLocationField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosOptionChip
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSelectField
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosSupportingText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTextArea
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTextField
import io.github.themonstersp4.mejengueros.ui.components.SelectedLocation
import io.github.themonstersp4.mejengueros.ui.components.clearFocusOnTap

data class CreateComplexScreenActions(
    val onRetryCatalogs: () -> Unit,
    val onRetryCantons: () -> Unit,
    val onComplexNameChange: (String) -> Unit,
    val onProvinceSelected: (String) -> Unit,
    val onCantonSelected: (String) -> Unit,
    val onComplexAddressChange: (String) -> Unit,
    val onOpenLocationPicker: () -> Unit,
    val onClearLocation: () -> Unit,
    val onToggleComplexService: (String) -> Unit,
    val onFirstCourtNameChange: (String) -> Unit,
    val onToggleCourtService: (String) -> Unit,
    val onPickCourtImage: () -> Unit,
    val onClearCourtImage: () -> Unit,
    val onNext: () -> Unit,
    val onBack: () -> Unit,
    val onSubmit: () -> Unit,
    val onSuccessAcknowledged: () -> Unit,
)

@Composable
fun CreateComplexScreen(
    state: CreateComplexUiState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    actions: CreateComplexScreenActions,
) {
  val selectedLocation =
      if (state.latitude != null && state.longitude != null) {
        SelectedLocation(latitude = state.latitude, longitude = state.longitude)
      } else {
        null
      }

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .clearFocusOnTap()
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
              "Completá el wizard de 2 pasos para registrar el complejo, sus servicios y la primera cancha.",
      )
    }

    WizardStepCard(currentStep = state.currentStep)

    if (state.currentStep == CreateComplexStep.Complex) {
      ComplexStepContent(
          state = state,
          selectedLocation = selectedLocation,
          onComplexNameChange = actions.onComplexNameChange,
          onProvinceSelected = actions.onProvinceSelected,
          onCantonSelected = actions.onCantonSelected,
          onComplexAddressChange = actions.onComplexAddressChange,
          onOpenLocationPicker = actions.onOpenLocationPicker,
          onClearLocation = actions.onClearLocation,
          onToggleComplexService = actions.onToggleComplexService,
      )
    } else {
      FirstCourtStepContent(
          state = state,
          onFirstCourtNameChange = actions.onFirstCourtNameChange,
          onToggleCourtService = actions.onToggleCourtService,
          onPickCourtImage = actions.onPickCourtImage,
          onClearCourtImage = actions.onClearCourtImage,
      )
    }

    state.errorMessage?.let { message ->
      MejenguerosErrorText(
          text = message,
          textAlign = TextAlign.Start,
          modifier = Modifier.testTag("create_complex_error"),
      )

      if (state.hasCatalogLoadFailure) {
        MejenguerosFullWidthOutlinedButton(
            text = if (state.isLoadingCatalogs) "Reintentando..." else "Reintentar",
            onClick = actions.onRetryCatalogs,
            enabled = !state.isLoadingCatalogs,
            modifier = Modifier.testTag("create_complex_retry_catalogs_button"),
        )
      } else if (state.hasCantonLoadFailure) {
        MejenguerosFullWidthOutlinedButton(
            text =
                if (state.isLoadingCantons) "Reintentando cantones..." else "Reintentar cantones",
            onClick = actions.onRetryCantons,
            enabled = !state.isLoadingCantons,
            modifier = Modifier.testTag("create_complex_retry_cantons_button"),
        )
      }
    }

    if (state.currentStep == CreateComplexStep.Complex) {
      MejenguerosFullWidthPrimaryButton(
          text = "Continuar con la primera cancha",
          onClick = actions.onNext,
          enabled = state.canGoToCourtStep,
          modifier = Modifier.testTag("create_complex_next_button"),
      )
    } else {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MejenguerosFullWidthOutlinedButton(
            text = "Volver",
            onClick = actions.onBack,
            enabled = !state.isSubmitting,
            modifier = Modifier.weight(1f).testTag("create_complex_back_button"),
        )
        MejenguerosFullWidthPrimaryButton(
            text = if (state.isSubmitting) "Creando..." else "Crear complejo",
            onClick = actions.onSubmit,
            enabled = state.canSubmit,
            modifier = Modifier.weight(1f).testTag("create_complex_submit_button"),
        )
      }
    }

    state.successMessage?.let { message ->
      val createdComplex = state.createdComplex
      val dialogMessage =
          if (createdComplex == null) {
            message
          } else {
            "$message\n\n${createdComplex.complexName} · ${createdComplex.complexAddress}\nPrimera cancha: ${createdComplex.firstCourtName}"
          }
      MejenguerosConfirmationDialog(
          title = "Complejo creado",
          message = dialogMessage,
          confirmText = "Aceptar",
          onConfirm = actions.onSuccessAcknowledged,
          onDismissRequest = {},
          modifier = Modifier.testTag("create_complex_success_dialog"),
      )
    }
  }
}

@Composable
private fun ComplexStepContent(
    state: CreateComplexUiState,
    selectedLocation: SelectedLocation?,
    onComplexNameChange: (String) -> Unit,
    onProvinceSelected: (String) -> Unit,
    onCantonSelected: (String) -> Unit,
    onComplexAddressChange: (String) -> Unit,
    onOpenLocationPicker: () -> Unit,
    onClearLocation: () -> Unit,
    onToggleComplexService: (String) -> Unit,
) {
  MejenguerosFormStack(verticalSpacing = 16.dp) {
    MejenguerosTextField(
        value = state.complexName,
        onValueChange = onComplexNameChange,
        label = "Nombre del complejo",
        enabled = !state.isSubmitting,
    )
    MejenguerosSelectField(
        value = state.selectedProvince?.toOptionLabel().orEmpty(),
        label = "Provincia",
        options = state.provinces.map { it.toOptionLabel() },
        onOptionSelected = { label ->
          state.provinces
              .firstOrNull { it.toOptionLabel() == label }
              ?.let { onProvinceSelected(it.id) }
        },
        enabled = !state.isLoadingCatalogs && !state.isSubmitting,
        supportingText = if (state.isLoadingCatalogs) "Cargando provincias..." else null,
        modifier = Modifier.testTag("province_select_field"),
    )
    MejenguerosSelectField(
        value = state.selectedCanton?.toOptionLabel().orEmpty(),
        label = "Cantón",
        options = state.cantons.map { it.toOptionLabel() },
        onOptionSelected = { label ->
          state.cantons.firstOrNull { it.toOptionLabel() == label }?.let { onCantonSelected(it.id) }
        },
        enabled =
            state.selectedProvinceId != null &&
                !state.isLoadingCantons &&
                !state.isLoadingCatalogs &&
                !state.isSubmitting,
        supportingText =
            when {
              state.selectedProvinceId == null -> "Elegí una provincia primero."
              state.isLoadingCantons -> "Cargando cantones..."
              state.cantons.isEmpty() -> "No hay cantones disponibles para la provincia elegida."
              else -> null
            },
        modifier = Modifier.testTag("canton_select_field"),
    )
    MejenguerosTextArea(
        value = state.complexAddress,
        onValueChange = onComplexAddressChange,
        label = "Dirección",
        enabled = !state.isSubmitting,
    )

    MejenguerosLocationField(
        selectedLocation = selectedLocation,
        onOpenPicker = onOpenLocationPicker,
    )
    if (selectedLocation != null) {
      MejenguerosFullWidthOutlinedButton(
          text = "Quitar ubicación",
          onClick = onClearLocation,
          enabled = !state.isSubmitting,
      )
    }

    ServiceSelectionSection(
        title = "Servicios del complejo",
        description = "Opcional. Elegí los servicios que ya ofrece el complejo.",
        services = state.complexServices,
        selectedIds = state.selectedComplexServiceIds,
        onToggleService = onToggleComplexService,
        enabled = !state.isLoadingCatalogs && !state.isSubmitting,
    )
  }
}

@Composable
private fun FirstCourtStepContent(
    state: CreateComplexUiState,
    onFirstCourtNameChange: (String) -> Unit,
    onToggleCourtService: (String) -> Unit,
    onPickCourtImage: () -> Unit,
    onClearCourtImage: () -> Unit,
) {
  MejenguerosFormStack(verticalSpacing = 16.dp) {
    MejenguerosTextField(
        value = state.firstCourtName,
        onValueChange = onFirstCourtNameChange,
        label = "Nombre de la primera cancha",
        enabled = !state.isSubmitting,
    )
    ServiceSelectionSection(
        title = "Servicios de la cancha",
        description =
            "Obligatorio. Elegí al menos un servicio de cancha; el tipo de césped también se modela aquí.",
        services = state.courtServices,
        selectedIds = state.selectedCourtServiceIds,
        onToggleService = onToggleCourtService,
        enabled = !state.isLoadingCatalogs && !state.isSubmitting,
        required = true,
    )
    CourtImageSection(
        isPickerAvailable = state.isCourtImagePickerAvailable,
        selectedCourtImage = state.selectedCourtImage,
        onPickCourtImage = onPickCourtImage,
        onClearCourtImage = onClearCourtImage,
        pickButtonTag = "create_complex_pick_court_image_button",
        clearButtonTag = "create_complex_clear_court_image_button",
        previewTag = "create_complex_court_image_preview",
        enabled = !state.isSubmitting,
    )
  }
}

@Composable
private fun CourtImageSection(
    isPickerAvailable: Boolean,
    selectedCourtImage: LocalCourtImage?,
    onPickCourtImage: () -> Unit,
    onClearCourtImage: () -> Unit,
    pickButtonTag: String,
    clearButtonTag: String,
    previewTag: String,
    enabled: Boolean,
) {
  if (!isPickerAvailable && selectedCourtImage == null) {
    return
  }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
        text = "Imagen de la cancha",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground,
    )
    MejenguerosSupportingText(
        text = "Opcional. Podés agregar una imagen ahora o dejarla para más adelante.",
    )

    selectedCourtImage?.let { courtImage ->
      Card(modifier = Modifier.fillMaxWidth().testTag(previewTag)) {
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
        modifier = Modifier.testTag(pickButtonTag),
    )

    if (selectedCourtImage != null) {
      MejenguerosFullWidthOutlinedButton(
          text = "Quitar imagen",
          onClick = onClearCourtImage,
          enabled = enabled,
          modifier = Modifier.testTag(clearButtonTag),
      )
    }
  }
}

@Composable
private fun ServiceSelectionSection(
    title: String,
    description: String,
    services: List<ServiceCatalogItem>,
    selectedIds: List<String>,
    onToggleService: (String) -> Unit,
    enabled: Boolean,
    required: Boolean = false,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground,
    )
    MejenguerosSupportingText(text = description)

    if (services.isEmpty()) {
      MejenguerosSupportingText(
          text =
              if (enabled) "No hay servicios disponibles en este momento."
              else "Cargando servicios...",
      )
    } else {
      FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        services.forEach { service ->
          MejenguerosOptionChip(
              text = service.name,
              selected = service.id in selectedIds,
              onClick = { onToggleService(service.id) },
              enabled = enabled,
              modifier = Modifier.testTag("service_chip_${service.id}"),
          )
        }
      }
    }

    if (required) {
      MejenguerosSupportingText(text = "Seleccionados: ${selectedIds.size}")
    }
  }
}

@Composable
private fun WizardStepCard(currentStep: CreateComplexStep) {
  Card {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
          text = if (currentStep == CreateComplexStep.Complex) "Paso 1 de 2" else "Paso 2 de 2",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
      )
      Text(
          text =
              if (currentStep == CreateComplexStep.Complex) "Datos del complejo"
              else "Primera cancha",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

private fun Province.toOptionLabel(): String = "$code · $name"

private fun Canton.toOptionLabel(): String = "$code · $name"
