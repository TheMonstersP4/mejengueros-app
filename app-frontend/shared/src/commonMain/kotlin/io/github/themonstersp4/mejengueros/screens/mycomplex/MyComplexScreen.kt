package io.github.themonstersp4.mejengueros.screens.mycomplex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.themonstersp4.mejengueros.domain.model.CourtAvailabilitySetupStatus
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubComplex
import io.github.themonstersp4.mejengueros.domain.model.MyComplexHubCourt
import io.github.themonstersp4.mejengueros.navigation.OwnerCourtAvailabilityEntrypoint
import io.github.themonstersp4.mejengueros.presentation.mycomplex.MyComplexUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosErrorText
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosInlineLoadingState
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
    courtImageErrorMessage: String? = null,
    isCourtImagePickerAvailable: Boolean = false,
    isUpdatingCourtImage: Boolean = false,
    contentPadding: PaddingValues,
    onRetry: () -> Unit,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
    onPickCourtImage: (String) -> Unit = {},
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
              courtImageErrorMessage = courtImageErrorMessage,
              isCourtImagePickerAvailable = isCourtImagePickerAvailable,
              isUpdatingCourtImage = isUpdatingCourtImage,
              onConfigureAvailability = onConfigureAvailability,
              onPickCourtImage = onPickCourtImage,
          )
      isLoading -> LoadingState()
      errorMessage != null -> ErrorState(errorMessage, onRetry)
      else -> MissingComplexState()
    }
  }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
  Box(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
  ) {
    MejenguerosInlineLoadingState(
        text = "Cargando tu hub de complejos…",
        containerTestTag = "my_complex_loading",
        indicatorTestTag = "my_complex_loading_indicator",
    )
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
    courtImageErrorMessage: String?,
    isCourtImagePickerAvailable: Boolean,
    isUpdatingCourtImage: Boolean,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
    onPickCourtImage: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
    if (isRefreshing) {
      Text(
          text = "Actualizando complejo...",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    courtImageErrorMessage?.let { message ->
      MejenguerosErrorText(
          text = message,
          modifier = Modifier.testTag("complex_detail_image_error"),
      )
    }
    ComplexHubSection(
        complex = complex,
        isCourtImagePickerAvailable = isCourtImagePickerAvailable,
        isUpdatingCourtImage = isUpdatingCourtImage,
        onConfigureAvailability = onConfigureAvailability,
        onPickCourtImage = onPickCourtImage,
    )
  }
}

@Composable
private fun ComplexHubSection(
    complex: MyComplexHubComplex,
    isCourtImagePickerAvailable: Boolean,
    isUpdatingCourtImage: Boolean,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
    onPickCourtImage: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    ComplexSummaryCard(complex = complex)
    SectionLabel(text = "TUS CANCHAS")
    CourtsGroup(
        complex = complex,
        isCourtImagePickerAvailable = isCourtImagePickerAvailable,
        isUpdatingCourtImage = isUpdatingCourtImage,
        onConfigureAvailability = onConfigureAvailability,
        onPickCourtImage = onPickCourtImage,
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
    isCourtImagePickerAvailable: Boolean,
    isUpdatingCourtImage: Boolean,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
    onPickCourtImage: (String) -> Unit,
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
          isCourtImagePickerAvailable = isCourtImagePickerAvailable,
          isUpdatingCourtImage = isUpdatingCourtImage,
          showDivider = index < complex.courts.lastIndex,
          onConfigureAvailability = onConfigureAvailability,
          onPickCourtImage = onPickCourtImage,
      )
    }
  }
}

@Composable
private fun CourtRow(
    complexName: String,
    court: MyComplexHubCourt,
    isCourtImagePickerAvailable: Boolean,
    isUpdatingCourtImage: Boolean,
    showDivider: Boolean,
    onConfigureAvailability: (OwnerCourtAvailabilityEntrypoint) -> Unit,
    onPickCourtImage: (String) -> Unit,
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                  Text(
                      text = court.toSupportingText(),
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  CourtImageManagementSection(
                      court = court,
                      isCourtImagePickerAvailable = isCourtImagePickerAvailable,
                      isUpdatingCourtImage = isUpdatingCourtImage,
                      onPickCourtImage = onPickCourtImage,
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
                    TextButton(
                        onClick = {
                          onConfigureAvailability(
                              OwnerCourtAvailabilityEntrypoint(
                                  courtId = court.id,
                                  courtName = court.name,
                                  complexName = complexName,
                              )
                          )
                        },
                        modifier =
                            Modifier.testTag("my_complex_court_availability_button_${court.id}"),
                    ) {
                      Text(text = "Disponibilidad")
                    }
                  }
                }
              },
          ),
      modifier = Modifier.testTag("my_complex_court_row_${court.id}"),
      style =
          MejenguerosListItemStyle(
              showDivider = showDivider,
              shape = RectangleShape,
              dividerModifier = Modifier.testTag("my_complex_court_divider_${court.id}"),
          ),
  )
}

@Composable
private fun CourtImageManagementSection(
    court: MyComplexHubCourt,
    isCourtImagePickerAvailable: Boolean,
    isUpdatingCourtImage: Boolean,
    onPickCourtImage: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    CourtImagePreview(court = court)

    if (isCourtImagePickerAvailable) {
      TextButton(
          onClick = { onPickCourtImage(court.id) },
          enabled = !isUpdatingCourtImage,
          modifier = Modifier.testTag("my_complex_court_image_button_${court.id}"),
      ) {
        Text(text = if (court.imageUrl.isNullOrBlank()) "Agregar imagen" else "Cambiar imagen")
      }
    }
  }
}

@Composable
private fun CourtImagePreview(court: MyComplexHubCourt) {
  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .aspectRatio(16f / 9f)
              .testTag("my_complex_court_image_container_${court.id}"),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHighest,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    if (court.imageUrl.isNullOrBlank()) {
      Column(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
      ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sin imagen",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
      }
    } else {
      AsyncImage(
          model = court.imageUrl,
          contentDescription = "Imagen de ${court.name}",
          modifier = Modifier.fillMaxSize().testTag("my_complex_court_image_${court.id}"),
      )
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
