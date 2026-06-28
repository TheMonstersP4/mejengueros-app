package io.github.themonstersp4.mejengueros.screens.mycomplex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.navigation.OwnerCourtAvailabilityEntrypoint
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton

@Composable
fun MyComplexScreen(
    state: MyComplexUiState,
    username: String,
    contentPadding: PaddingValues,
    onCreateComplex: () -> Unit,
    onRetry: () -> Unit,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(contentPadding)
              .padding(horizontal = 20.dp, vertical = 24.dp)
              .verticalScroll(rememberScrollState())
              .testTag("my_complex_root"),
      verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
          text = "Mi complejo",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
          text = "Administrá tu operación como $username",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onBackground,
      )
    }

    when {
      state.isLoading -> LoadingState()
      state.errorMessage != null -> ErrorState(state.errorMessage, onRetry)
      state.isEmpty -> EmptyState(onCreateComplex)
      else -> LoadedState(state.complexes, onCreateComplex, onConfigureAvailability)
    }
  }
}

@Composable
private fun LoadingState() {
  Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    CircularProgressIndicator(modifier = Modifier.testTag("my_complex_loading_indicator"))
    Text(
        text = "Cargando tu hub de complejos...",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
  }
}

@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    MejenguerosErrorText(text = errorMessage, modifier = Modifier.testTag("my_complex_error"))
    MejenguerosFullWidthOutlinedButton(text = "Reintentar", onClick = onRetry)
  }
}

@Composable
private fun EmptyState(onCreateComplex: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
        text = "Todavía no tenés complejos creados.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text =
            "Creá tu primer complejo y su primera cancha para empezar a configurar disponibilidad y administrar tu operación.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    MejenguerosFullWidthPrimaryButton(
        text = "Crear complejo y primera cancha",
        onClick = onCreateComplex,
        modifier = Modifier.testTag("my_complex_create_complex_button"),
    )
  }
}

@Composable
private fun LoadedState(
    complexes: List<MyComplexHubComplex>,
    onCreateComplex: () -> Unit,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    MejenguerosFullWidthPrimaryButton(
        text = "Crear complejo y primera cancha",
        onClick = onCreateComplex,
        modifier = Modifier.testTag("my_complex_create_complex_button"),
    )

    complexes.forEach { complex ->
      ComplexCard(
          complex = complex,
          onConfigureAvailability = onConfigureAvailability,
      )
    }
  }
}

@Composable
private fun ComplexCard(
    complex: MyComplexHubComplex,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
) {
  Card(modifier = Modifier.fillMaxWidth().testTag("my_complex_card_${complex.id}")) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
          text = complex.name,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
          text = complex.address,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
      )
      complex.toLocationLabel()?.let { locationLabel ->
        Text(
            text = locationLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (complex.courts.isEmpty()) {
        Text(
            text = "Todavía no hay canchas cargadas para este complejo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
      } else {
        complex.courts.forEach { court ->
          CourtCard(
              complexName = complex.name,
              court = court,
              onConfigureAvailability = onConfigureAvailability,
          )
        }
      }
    }
  }
}

@Composable
private fun CourtCard(
    complexName: String,
    court: MyComplexHubCourt,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
) {
  Card(modifier = Modifier.fillMaxWidth().testTag("my_complex_court_card_${court.id}")) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
          text = court.name,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
          text = court.availabilityStatus.toLabel(),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
          text = "Estado de cancha: ${court.status}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      MejenguerosFullWidthOutlinedButton(
          text =
              if (court.availabilityStatus == CourtAvailabilitySetupStatus.CONFIGURED) {
                "Editar disponibilidad"
              } else {
                "Configurar disponibilidad"
              },
          onClick = {
            onConfigureAvailability(
                OwnerCourtAvailabilityEntrypoint(
                    courtId = court.id,
                    courtName = court.name,
                    complexName = complexName,
                )
            )
          },
          modifier = Modifier.testTag("my_complex_configure_availability_button_${court.id}"),
      )
    }
  }
}

private fun MyComplexHubComplex.toLocationLabel(): String? {
  if (latitude == null || longitude == null) {
    return null
  }

  return "Ubicación: $latitude, $longitude"
}

private fun CourtAvailabilitySetupStatus.toLabel(): String =
    when (this) {
      CourtAvailabilitySetupStatus.CONFIGURED -> "Disponibilidad configurada"
      CourtAvailabilitySetupStatus.PENDING -> "Disponibilidad pendiente"
    }
