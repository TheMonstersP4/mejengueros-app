package io.github.themonstersp4.mejengueros.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosCourtCard
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateContent
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateVariant
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: CourtCatalogUiState,
    contentPadding: PaddingValues,
    onSearchQueryChange: (String) -> Unit,
    onProvinceSelected: (String?) -> Unit,
    onCantonSelected: (String?) -> Unit,
    onRetryLoad: () -> Unit,
    onOpenCourtDetail: (CourtCatalogItem) -> Unit,
    modifier: Modifier = Modifier,
) {
  val hasCourts =
      !state.isLoading && state.loadErrorMessage == null && state.visibleCourts.isNotEmpty()

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(contentPadding)
              .background(MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    if (hasCourts) {
      // The header scrolls together with the court list in a single scroll container.
      LazyColumn(
          modifier = Modifier.fillMaxSize().testTag("catalog_court_list"),
          contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        item {
          CatalogHeader(
              state = state,
              onSearchQueryChange = onSearchQueryChange,
              onProvinceSelected = onProvinceSelected,
              onCantonSelected = onCantonSelected,
          )
        }
        items(state.visibleCourts, key = { it.id }) { court ->
          MejenguerosCourtCard(
              title = court.displayName,
              location = "${court.provinceName} · ${court.cantonName}",
              imageUrl = court.imageUrl,
              imageContentDescription = court.displayName,
              metadata = buildCourtMetadata(court),
              statusText = if (court.isReservableToday) "Reservable hoy" else null,
              modifier =
                  Modifier.padding(horizontal = 20.dp)
                      .testTag("catalog_court_card_${court.id}")
                      .semantics {
                        onClick {
                          onOpenCourtDetail(court)
                          true
                        }
                      },
              onClick = { onOpenCourtDetail(court) },
          )
        }
      }
    } else {
      // Keep the compact search header visible while the state message fills the rest.
      CatalogHeader(
          state = state,
          onSearchQueryChange = onSearchQueryChange,
          onProvinceSelected = onProvinceSelected,
          onCantonSelected = onCantonSelected,
      )
      Box(
          modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp),
          contentAlignment = Alignment.Center,
      ) {
        when {
          state.isLoading ->
              MejenguerosStateContent(
                  title = "Cargando canchas",
                  description = "Preparando el catálogo disponible.",
                  variant = MejenguerosStateVariant.Pending,
              )

          state.loadErrorMessage != null ->
              MejenguerosStateContent(
                  title = "Catálogo no disponible",
                  description = state.loadErrorMessage,
                  variant = MejenguerosStateVariant.Error,
                  actions = {
                    MejenguerosOutlinedButton(
                        text = "Reintentar",
                        onClick = onRetryLoad,
                        modifier =
                            Modifier.testTag("catalog_retry_button").semantics {
                              contentDescription = "Reintentar catálogo"
                            },
                    )
                  },
              )

          else ->
              MejenguerosStateContent(
                  title = "Sin resultados",
                  description = "Ajustá la búsqueda o los filtros para encontrar otra cancha.",
                  variant = MejenguerosStateVariant.Empty,
              )
        }
      }
    }
  }
}

@Composable
private fun CatalogHeader(
    state: CourtCatalogUiState,
    onSearchQueryChange: (String) -> Unit,
    onProvinceSelected: (String?) -> Unit,
    onCantonSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
  var provinceMenuExpanded by remember { mutableStateOf(false) }
  var cantonMenuExpanded by remember { mutableStateOf(false) }

  Column(
      modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Text(
        text = "Canchas",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )
    Box(
        modifier =
            Modifier.width(40.dp)
                .height(3.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(999.dp),
                )
    )
    SearchPill(
        query = state.searchQuery,
        onQueryChange = onSearchQueryChange,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChipSummary(
          label = state.selectedProvince?.label ?: "Provincia",
          selected = state.selectedProvince != null,
          onClick = { provinceMenuExpanded = true },
      ) {
        DropdownMenu(
            expanded = provinceMenuExpanded,
            onDismissRequest = { provinceMenuExpanded = false },
        ) {
          DropdownMenuItem(
              text = { Text("Todas") },
              onClick = {
                provinceMenuExpanded = false
                onProvinceSelected(null)
              },
          )
          state.availableProvinces.forEach { province ->
            DropdownMenuItem(
                text = { Text(province.label) },
                onClick = {
                  provinceMenuExpanded = false
                  onProvinceSelected(province.id)
                },
            )
          }
        }
      }
      FilterChipSummary(
          label = state.selectedCanton?.label ?: "Cantón",
          selected = state.selectedCanton != null,
          onClick = { cantonMenuExpanded = true },
      ) {
        DropdownMenu(
            expanded = cantonMenuExpanded,
            onDismissRequest = { cantonMenuExpanded = false },
        ) {
          DropdownMenuItem(
              text = { Text("Todos") },
              onClick = {
                cantonMenuExpanded = false
                onCantonSelected(null)
              },
          )
          state.availableCantons.forEach { canton ->
            DropdownMenuItem(
                text = { Text(canton.label) },
                onClick = {
                  cantonMenuExpanded = false
                  onCantonSelected(canton.id)
                },
            )
          }
        }
      }
    }
  }
}

private fun buildCourtMetadata(court: CourtCatalogItem): List<String> {
  val metadata = mutableListOf<String>()

  court.services.firstOrNull()?.let(metadata::add)

  ratingLabel(court.ratingAverage, court.ratingCount)?.let(metadata::add)

  if (metadata.size < 2) {
    court.services.drop(1).firstOrNull()?.let(metadata::add)
  }

  if (metadata.isEmpty()) {
    metadata.add("Servicios por confirmar")
  }

  return metadata.distinct().take(2)
}

private fun ratingLabel(average: Double?, count: Int): String? {
  if (average == null || count <= 0) {
    return null
  }

  val rounded = (average * 10).roundToInt() / 10.0
  val averageLabel = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()

  return "★ $averageLabel · $count reseñas"
}

@Composable
private fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
) {
  OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      shape = CircleShape,
      placeholder = { Text("Buscar cancha o complejo") },
      leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
      colors =
          TextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              disabledContainerColor = MaterialTheme.colorScheme.surface,
              focusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
              unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
              focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
  )
}

@Composable
private fun FilterChipSummary(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuContent: @Composable () -> Unit = {},
) {
  Box(modifier = modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color =
            if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        contentColor =
            if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        border =
            BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.outlineVariant,
            ),
    ) {
      Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
      }
    }
    menuContent()
  }
}
