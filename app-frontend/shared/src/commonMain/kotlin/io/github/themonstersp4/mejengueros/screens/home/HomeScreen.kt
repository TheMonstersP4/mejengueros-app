package io.github.themonstersp4.mejengueros.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.domain.model.CourtCatalogItem
import io.github.themonstersp4.mejengueros.presentation.catalog.CatalogFilterOption
import io.github.themonstersp4.mejengueros.presentation.catalog.CourtCatalogUiState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosCourtCard
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosInlineLoadingState
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateContent
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateVariant
import kotlin.math.roundToInt

/**
 * The next page loads once the user actually reaches the end of the list (the last item becomes
 * visible) rather than prefetching ahead, so each page load is a perceptible "reached the bottom ->
 * load more" event with a visible loading footer.
 */
private const val NextPageLoadThreshold = 1

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
    onServiceToggled: (String) -> Unit = {},
    onServicesCleared: () -> Unit = {},
    onMinRatingSelected: (Int?) -> Unit = {},
    onClearAllFilters: () -> Unit = {},
    onLoadNextPage: () -> Unit = {},
    onRetryNextPage: () -> Unit = {},
) {
  val hasCourts =
      !state.isLoading && state.loadErrorMessage == null && state.visibleCourts.isNotEmpty()
  var filtersVisible by remember { mutableStateOf(false) }
  // Intentionally not rememberSaveable: the catalog refetches from page 1 on a
  // cold start, so restoring a deep scroll position would make the list chase
  // it by loading every intermediate page at once. Start fresh at the top.
  val listState = remember { LazyListState() }

  // Load the next page only once the user reaches the end of the list, so the
  // catalog paginates in perceptible steps instead of streaming ahead.
  val shouldLoadNextPage by remember {
    derivedStateOf {
      val layoutInfo = listState.layoutInfo
      val totalItems = layoutInfo.totalItemsCount
      val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
      totalItems > 0 && lastVisibleIndex >= totalItems - NextPageLoadThreshold
    }
  }
  LaunchedEffect(shouldLoadNextPage, state.canLoadNextPage) {
    if (shouldLoadNextPage && state.canLoadNextPage) {
      onLoadNextPage()
    }
  }

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .padding(contentPadding)
              .background(MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    // A single always-present scroll container keeps the search field at one stable
    // call site. Branching the header between two parents (the list vs. the state
    // message) would dispose and recreate its text field on every loading toggle,
    // stealing focus on each keystroke while the search debounce refetches page 1.
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag("catalog_court_list"),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item(key = "catalog_header") {
        CatalogHeader(
            state = state,
            onSearchQueryChange = onSearchQueryChange,
            onOpenFilters = { filtersVisible = true },
        )
      }

      if (hasCourts) {
        items(state.visibleCourts, key = { it.id }) { court ->
          MejenguerosCourtCard(
              title = court.displayTitle,
              location = catalogCourtContext(court),
              imageUrl = court.imageUrl,
              imageContentDescription = court.displayTitle,
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
        // Only surface the footer once it has something to say: a page is
        // loading, a page failed, or the user reached the end after paging.
        val reachedEndAfterPaging = !state.hasNextPage && state.currentPage > 1
        if (
            state.isLoadingNextPage || state.nextPageErrorMessage != null || reachedEndAfterPaging
        ) {
          item(key = "catalog_pagination_footer") {
            CatalogPaginationFooter(
                isLoadingNextPage = state.isLoadingNextPage,
                nextPageErrorMessage = state.nextPageErrorMessage,
                hasNextPage = state.hasNextPage,
                onRetryNextPage = onRetryNextPage,
            )
          }
        }
      } else {
        // Loading, error, and empty states live inside the same list so the header
        // above them never changes call site.
        item(key = "catalog_state") {
          Box(
              modifier = Modifier.fillParentMaxWidth().fillParentMaxHeight().padding(20.dp),
              contentAlignment = Alignment.Center,
          ) {
            when {
              state.isLoading ->
                  MejenguerosInlineLoadingState(
                      text = "Cargando canchas…",
                      modifier = Modifier.fillMaxWidth(),
                      containerTestTag = "catalog_loading",
                      indicatorTestTag = "catalog_loading_indicator",
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

    if (filtersVisible) {
      CatalogFiltersSheet(
          state = state,
          onDismiss = { filtersVisible = false },
          onProvinceSelected = onProvinceSelected,
          onCantonSelected = onCantonSelected,
          onServiceToggled = onServiceToggled,
          onServicesCleared = onServicesCleared,
          onMinRatingSelected = onMinRatingSelected,
          onClearAllFilters = onClearAllFilters,
      )
    }
  }
}

/**
 * Trailing item of the catalog list. Surfaces the incremental-load lifecycle: a spinner while the
 * next page loads, an inline retry when it fails, and an end-of-list marker once every page has
 * been consumed.
 */
@Composable
private fun CatalogPaginationFooter(
    isLoadingNextPage: Boolean,
    nextPageErrorMessage: String?,
    hasNextPage: Boolean,
    onRetryNextPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(
      modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center,
  ) {
    when {
      isLoadingNextPage ->
          MejenguerosInlineLoadingState(
              text = "Cargando más canchas…",
              modifier = Modifier.fillMaxWidth(),
              containerTestTag = "catalog_next_page_loading",
              indicatorTestTag = "catalog_next_page_loading_indicator",
          )

      nextPageErrorMessage != null ->
          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
                text = nextPageErrorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MejenguerosOutlinedButton(
                text = "Reintentar",
                onClick = onRetryNextPage,
                modifier =
                    Modifier.testTag("catalog_next_page_retry_button").semantics {
                      contentDescription = "Reintentar carga de más canchas"
                    },
            )
          }

      !hasNextPage ->
          Text(
              text = "No hay más canchas.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("catalog_end_of_list"),
          )
    }
  }
}

@Composable
private fun CatalogHeader(
    state: CourtCatalogUiState,
    onSearchQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    SearchPill(
        query = state.searchQuery,
        onQueryChange = onSearchQueryChange,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      FiltersButton(
          activeCount = state.activeFilterCount,
          onClick = onOpenFilters,
      )
      if (state.totalCourts > 0) {
        Text(
            text = "Mostrando ${state.visibleCourts.size} de ${state.totalCourts}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("catalog_count_indicator"),
        )
      }
    }
  }
}

/** Single entry point that opens the filters bottom sheet, badged with the active filter count. */
@Composable
private fun FiltersButton(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val selected = activeCount > 0
  Surface(
      onClick = onClick,
      modifier = modifier.testTag("catalog_filters_button"),
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          text = if (selected) "Filtros ($activeCount)" else "Filtros",
          style = MaterialTheme.typography.titleSmall,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CatalogFiltersSheet(
    state: CourtCatalogUiState,
    onDismiss: () -> Unit,
    onProvinceSelected: (String?) -> Unit,
    onCantonSelected: (String?) -> Unit,
    onServiceToggled: (String) -> Unit,
    onServicesCleared: () -> Unit,
    onMinRatingSelected: (Int?) -> Unit,
    onClearAllFilters: () -> Unit,
) {
  // Skip the half-expanded stop so the panel opens fully in one gesture and stays
  // deterministic under tests.
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      modifier = Modifier.testTag("catalog_filters_sheet"),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = "Filtros", style = MaterialTheme.typography.titleLarge)
        if (state.activeFilterCount > 0) {
          TextButton(
              onClick = onClearAllFilters,
              modifier = Modifier.testTag("catalog_filters_clear"),
          ) {
            Text("Limpiar todo")
          }
        }
      }

      FilterSection(title = "Provincia") {
        SingleChoiceChips(
            options = state.availableProvinces,
            selectedId = state.selectedProvinceId,
            onSelected = onProvinceSelected,
        )
      }

      if (state.availableCantons.isNotEmpty()) {
        FilterSection(title = "Cantón") {
          SingleChoiceChips(
              options = state.availableCantons,
              selectedId = state.selectedCantonId,
              onSelected = onCantonSelected,
          )
        }
      }

      if (state.availableServices.isNotEmpty()) {
        FilterSection(title = "Servicios") {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.availableServices.forEach { service ->
              val isSelected = service.id in state.selectedServiceIds
              FilterChip(
                  selected = isSelected,
                  onClick = { onServiceToggled(service.id) },
                  label = { Text(service.label) },
                  leadingIcon =
                      if (isSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                      } else {
                        null
                      },
              )
            }
          }
        }
      }

      FilterSection(title = "Calificación") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
              selected = state.selectedMinRating == null,
              onClick = { onMinRatingSelected(null) },
              label = { Text("Todas") },
          )
          MinRatingOptions.forEach { minRating ->
            FilterChip(
                selected = state.selectedMinRating == minRating,
                onClick = { onMinRatingSelected(minRating) },
                label = { Text(ratingOptionLabel(minRating)) },
            )
          }
        }
      }

      MejenguerosFullWidthPrimaryButton(
          text = "Ver ${state.totalCourts} canchas",
          onClick = onDismiss,
          modifier = Modifier.testTag("catalog_filters_apply"),
      )
    }
  }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(text = title, style = MaterialTheme.typography.titleSmall)
    content()
  }
}

/** A row of mutually exclusive chips with an always-present "Todas" reset option. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleChoiceChips(
    options: List<CatalogFilterOption>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    FilterChip(
        selected = selectedId == null,
        onClick = { onSelected(null) },
        label = { Text("Todas") },
    )
    options.forEach { option ->
      FilterChip(
          selected = option.id == selectedId,
          onClick = { onSelected(option.id) },
          label = { Text(option.label) },
      )
    }
  }
}

private fun catalogCourtContext(court: CourtCatalogItem): String =
    listOf(court.complexName, court.provinceName, court.cantonName)
        .filter { it.isNotBlank() }
        .joinToString(separator = " · ")

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

// Rating filter thresholds offered in the search header, highest first.
private val MinRatingOptions = listOf(5, 4, 3)

// 5 is the ceiling so it reads as an exact match; lower thresholds are inclusive.
private fun ratingOptionLabel(minRating: Int): String =
    if (minRating >= 5) "$minRating★" else "$minRating★+"

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
