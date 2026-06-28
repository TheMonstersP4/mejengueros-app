package io.github.themonstersp4.mejengueros.screens.mycomplex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.navigation.OwnerCourtAvailabilityEntrypoint
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListGroup
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListItem
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPill
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPillStyle

@Composable
fun MyComplexScreen(
    state: MyComplexUiState,
    username: String,
    contentPadding: PaddingValues,
    onCreateComplex: () -> Unit,
    onRetry: () -> Unit,
    onOpenComplexDetail: (String) -> Unit,
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
      else ->
          ComplexListState(complexes = state.complexes, onOpenComplexDetail = onOpenComplexDetail)
    }
  }
}

@Composable
fun ComplexDetailScreen(
    complex: MyComplexHubComplex?,
    isLoading: Boolean,
    errorMessage: String?,
    contentPadding: PaddingValues,
    onRetry: () -> Unit,
    onAddCourt: (String, String) -> Unit,
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
              .testTag("complex_detail_root"),
      verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    when {
      complex != null ->
          ComplexDetailContent(
              complex = complex,
              isRefreshing = isLoading,
              onAddCourt = onAddCourt,
              onConfigureAvailability = onConfigureAvailability,
          )
      isLoading -> LoadingState()
      errorMessage != null -> ErrorState(errorMessage, onRetry)
      else -> MissingComplexState()
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
private fun ErrorState(errorMessage: String, onRetry: () -> Unit) {
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
private fun MissingComplexState() {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
        text = "No encontramos el complejo seleccionado.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = "Volvé al listado y elegí otro complejo para continuar.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
  }
}

@Composable
private fun ComplexListState(
    complexes: List<MyComplexHubComplex>,
    onOpenComplexDetail: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionLabel(text = "TUS COMPLEJOS")
    MejenguerosListGroup {
      complexes.forEachIndexed { index, complex ->
        MejenguerosListItem(
            title = complex.name,
            supportingText = complex.toListSupportingText(),
            modifier = Modifier.testTag("my_complex_list_item_${complex.id}"),
            leading = { CircleEmojiIcon(symbol = "🏟️") },
            trailing = {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            onClick = { onOpenComplexDetail(complex.id) },
            showDivider = index < complexes.lastIndex,
        )
      }
    }
  }
}

@Composable
private fun ComplexDetailContent(
    complex: MyComplexHubComplex,
    isRefreshing: Boolean,
    onAddCourt: (String, String) -> Unit,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
    if (isRefreshing) {
      Text(
          text = "Actualizando complejo...",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    ComplexHubSection(
        complex = complex,
        onConfigureAvailability = onConfigureAvailability,
        onAddCourt = { onAddCourt(complex.id, complex.name) },
    )
  }
}

@Composable
private fun ComplexHubSection(
    complex: MyComplexHubComplex,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
    onAddCourt: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    ComplexSummaryCard(complex = complex)
    SectionLabel(text = "TUS CANCHAS")
    CourtsGroup(complex = complex, onConfigureAvailability = onConfigureAvailability)
    AddCourtCallToAction(
        onClick = onAddCourt,
        enabled = true,
        modifier = Modifier.testTag("complex_detail_add_court_button_${complex.id}"),
    )
  }
}

@Composable
private fun ComplexSummaryCard(complex: MyComplexHubComplex) {
  Card(modifier = Modifier.fillMaxWidth().testTag("my_complex_card_${complex.id}")) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
      complex.toStatusLabel()?.let { statusLabel ->
        MejenguerosStatusPill(text = statusLabel, style = MejenguerosStatusPillStyle.Subtle)
      }
    }
  }
}

@Composable
private fun CourtsGroup(
    complex: MyComplexHubComplex,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
) {
  if (complex.courts.isEmpty()) {
    MejenguerosListGroup {
      MejenguerosListItem(
          title = "Todavía no hay canchas cargadas",
          supportingText = "Cuando agregues la primera cancha aparecerá aquí.",
          enabled = false,
      )
    }
    return
  }

  MejenguerosListGroup {
    complex.courts.forEachIndexed { index, court ->
      CourtRow(
          complexName = complex.name,
          court = court,
          showDivider = index < complex.courts.lastIndex,
          onConfigureAvailability = onConfigureAvailability,
      )
    }
  }
}

@Composable
private fun CourtRow(
    complexName: String,
    court: MyComplexHubCourt,
    showDivider: Boolean,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
) {
  MejenguerosListItem(
      title = court.name,
      supportingText = court.toSupportingText(),
      modifier = Modifier.testTag("my_complex_court_row_${court.id}"),
      leading = { CircleEmojiIcon(symbol = "⚽") },
      trailing = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          MejenguerosStatusPill(
              text = court.availabilityStatus.toPillLabel(),
              style = court.availabilityStatus.toPillStyle(),
          )
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
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
      showDivider = showDivider,
  )
}

@Composable
private fun AddCourtCallToAction(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(20.dp)
  val borderColor = MaterialTheme.colorScheme.outlineVariant
  val contentColor =
      if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
  val dashedBorderModifier =
      Modifier.drawWithCache {
        val strokeWidth = 2.dp.toPx()
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(16.dp.toPx(), 12.dp.toPx()))
        onDrawBehind {
          drawRoundRect(
              color = borderColor,
              style = Stroke(width = strokeWidth, pathEffect = dashEffect),
              cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
          )
        }
      }

  Surface(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.fillMaxWidth().then(dashedBorderModifier),
      shape = shape,
      color = MaterialTheme.colorScheme.surface,
      contentColor = contentColor,
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          text = "+",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
          color = contentColor,
      )
      Spacer(modifier = Modifier.size(8.dp))
      Text(
          text = "Agregar cancha",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          textAlign = TextAlign.Center,
          color = contentColor,
      )
    }
  }
}

@Composable
private fun CircleEmojiIcon(symbol: String) {
  Surface(
      modifier = Modifier.size(40.dp),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surfaceContainer,
      contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(text = symbol, style = MaterialTheme.typography.titleMedium)
    }
  }
}

@Composable
private fun SectionLabel(text: String) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
  }
}

private fun MyComplexHubComplex.toLocationLabel(): String? =
    if (latitude == null || longitude == null) null else "Ubicación: $latitude, $longitude"

private fun MyComplexHubComplex.toStatusLabel(): String? =
    when (status.uppercase()) {
      "ACTIVE" -> "Activa"
      "INACTIVE" -> "Inactiva"
      else -> null
    }

private fun MyComplexHubComplex.toListSupportingText(): String {
  val courtsLabel = if (courts.size == 1) "1 cancha" else "${courts.size} canchas"
  return "$address · $courtsLabel"
}

private fun MyComplexHubCourt.toSupportingText(): String =
    when (availabilityStatus) {
      CourtAvailabilitySetupStatus.CONFIGURED -> "Activa · disponibilidad configurada"
      CourtAvailabilitySetupStatus.PENDING -> "Activa · falta disponibilidad"
    }

private fun CourtAvailabilitySetupStatus.toPillLabel(): String =
    when (this) {
      CourtAvailabilitySetupStatus.CONFIGURED -> "Configurada"
      CourtAvailabilitySetupStatus.PENDING -> "Pendiente"
    }

private fun CourtAvailabilitySetupStatus.toPillStyle(): MejenguerosStatusPillStyle =
    when (this) {
      CourtAvailabilitySetupStatus.CONFIGURED -> MejenguerosStatusPillStyle.Primary
      CourtAvailabilitySetupStatus.PENDING -> MejenguerosStatusPillStyle.Neutral
    }
