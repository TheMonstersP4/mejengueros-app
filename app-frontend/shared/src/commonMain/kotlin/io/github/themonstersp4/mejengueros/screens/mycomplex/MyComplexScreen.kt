package io.github.themonstersp4.mejengueros.screens.mycomplex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListItemCustomContent
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListItemStyle
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListItemText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPill
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPillStyle

@Composable
fun MyComplexScreen(
    state: MyComplexUiState,
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
              .testTag("my_complex_root"),
      verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
          text = "Tus complejos deportivos",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
          text = "Gestioná canchas, disponibilidad y reservas desde un solo lugar.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Box(modifier = Modifier.fillMaxSize()) {
      when {
        state.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
        state.errorMessage != null ->
            ScrollableStateContainer { ErrorState(state.errorMessage, onRetry) }
        state.isEmpty -> ScrollableStateContainer { EmptyState(onCreateComplex) }
        else ->
            ScrollableStateContainer {
              ComplexListState(
                  complexes = state.complexes,
                  onOpenComplexDetail = onOpenComplexDetail,
              )
            }
      }
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
private fun LoadingState(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
      Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        CircularProgressIndicator(modifier = Modifier.testTag("my_complex_loading_indicator"))
        Text(
            text = "Cargando tu hub de complejos...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun ScrollableStateContainer(content: @Composable ColumnScope.() -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      content = content,
  )
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
    MejenguerosListGroup(
        modifier = Modifier.testTag("my_complex_list_group"),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      complexes.forEachIndexed { index, complex ->
        MejenguerosListItem(
            text =
                MejenguerosListItemText(
                    title = complex.name,
                    supportingText = complex.toListSupportingText(),
                    headlineMaxLines = 2,
                    headlineOverflow = TextOverflow.Clip,
                    supportingMaxLines = 2,
                    headlineModifier = Modifier.testTag("my_complex_list_headline_${complex.id}"),
                ),
            modifier = Modifier.testTag("my_complex_list_item_${complex.id}"),
            leading = {
              Box(modifier = Modifier.testTag("my_complex_list_icon_${complex.id}")) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
              }
            },
            trailing = {
              Box(modifier = Modifier.testTag("my_complex_list_trailing_${complex.id}")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            },
            onClick = { onOpenComplexDetail(complex.id) },
            style =
                MejenguerosListItemStyle(
                    showDivider = index < complexes.lastIndex,
                    shape = RectangleShape,
                    dividerModifier = Modifier.testTag("my_complex_list_divider_${complex.id}"),
                ),
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
    Spacer(modifier = Modifier.height(8.dp))
    ActivitySection()
  }
}

@Composable
private fun ComplexSummaryCard(complex: MyComplexHubComplex) {
  Card(
      modifier = Modifier.fillMaxWidth().testTag("my_complex_card_${complex.id}"),
      shape = RoundedCornerShape(20.dp),
      colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = complex.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        complex.toStatusLabel()?.let { statusLabel ->
          MejenguerosStatusPill(text = statusLabel, style = MejenguerosStatusPillStyle.Subtle)
        }
      }
      Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.Top,
      ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
          }
        }
        Text(
            text = complex.address,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
          text =
              MejenguerosListItemText(
                  title = "Todavía no hay canchas cargadas",
                  supportingText = "Cuando agregues la primera cancha aparecerá aquí.",
              ),
          enabled = false,
      )
    }
    return
  }

  MejenguerosListGroup(modifier = Modifier.testTag("my_complex_courts_group")) {
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
      content =
          MejenguerosListItemCustomContent(
              headlineContent = {
                Text(
                    text = court.name,
                    modifier = Modifier.testTag("my_complex_court_headline_${court.id}"),
                    style =
                        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 3,
                    overflow = TextOverflow.Clip,
                )
              },
              supportingContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                      text = court.toSupportingText(),
                      modifier = Modifier.weight(1f),
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Row(
                      modifier = Modifier.testTag("my_complex_court_trailing_${court.id}"),
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
                }
              },
          ),
      modifier = Modifier.testTag("my_complex_court_row_${court.id}"),
      leading = {
        Box(modifier = Modifier.testTag("my_complex_court_icon_${court.id}")) {
          Surface(
              modifier = Modifier.size(36.dp),
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceContainerHighest,
              contentColor = MaterialTheme.colorScheme.primary,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                  imageVector = Icons.Filled.LocationOn,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
              )
            }
          }
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
      style =
          MejenguerosListItemStyle(
              showDivider = showDivider,
              shape = RectangleShape,
              dividerModifier = Modifier.testTag("my_complex_court_divider_${court.id}"),
          ),
  )
}

@Composable
private fun AddCourtCallToAction(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
  OutlinedButton(
      onClick = onClick,
      enabled = enabled,
      modifier = modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(20.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      colors =
          ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.primary,
              disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
  ) {
    Icon(
        imageVector = Icons.Filled.Add,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
        text = "Agregar cancha",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        textAlign = TextAlign.Center,
    )
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

@Composable
private fun ActivitySection() {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionLabel(text = "ACTIVIDAD")
    MejenguerosListGroup(modifier = Modifier.testTag("activity_section_root")) {
      ActivityPlaceholderRow(
          icon = Icons.Filled.Star,
          iconDescription = "Reseñas",
          title = "Reseñas recibidas",
          subtitle = "Lo que opinan los mejengueros",
          testTag = "activity_resenas_row",
          showDivider = true,
      )
      ActivityPlaceholderRow(
          icon = Icons.Filled.Check,
          iconDescription = "Reservas",
          title = "Reservas de mis canchas",
          subtitle = "Próximas y pasadas",
          testTag = "activity_reservas_row",
          showDivider = false,
      )
    }
  }
}

@Composable
private fun ActivityPlaceholderRow(
    icon: ImageVector,
    iconDescription: String?,
    title: String,
    subtitle: String,
    testTag: String,
    showDivider: Boolean,
) {
  MejenguerosListItem(
      text =
          MejenguerosListItemText(
              title = title,
              supportingText = subtitle,
          ),
      modifier = Modifier.testTag(testTag),
      leading = {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                modifier = Modifier.size(18.dp),
            )
          }
        }
      },
      trailing = {
        MejenguerosStatusPill(
            text = "Próximamente",
            style = MejenguerosStatusPillStyle.Neutral,
        )
      },
      style =
          MejenguerosListItemStyle(
              showDivider = showDivider,
              shape = RectangleShape,
          ),
  )
}

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
      CourtAvailabilitySetupStatus.CONFIGURED -> "Disponibilidad configurada"
      CourtAvailabilitySetupStatus.PENDING -> "Falta disponibilidad"
    }

private fun CourtAvailabilitySetupStatus.toPillLabel(): String =
    when (this) {
      CourtAvailabilitySetupStatus.CONFIGURED -> "Activa"
      CourtAvailabilitySetupStatus.PENDING -> "Pendiente"
    }

private fun CourtAvailabilitySetupStatus.toPillStyle(): MejenguerosStatusPillStyle =
    when (this) {
      CourtAvailabilitySetupStatus.CONFIGURED -> MejenguerosStatusPillStyle.Primary
      CourtAvailabilitySetupStatus.PENDING -> MejenguerosStatusPillStyle.Neutral
    }
