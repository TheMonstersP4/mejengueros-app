package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class MejenguerosLocationMapState {
  Loading,
  Ready,
  Unsupported,
  Error,
}

data class MejenguerosLocationPickerState(
    val draftLocation: SelectedLocation,
    val selectedLocation: SelectedLocation? = null,
)

data class MejenguerosLocationPickerActions(
    val onDraftLocationChange: (SelectedLocation) -> Unit,
    val onConfirm: (SelectedLocation) -> Unit,
    val onDismiss: () -> Unit,
)

data class MejenguerosLocationPickerMapScope(
    val draftLocation: SelectedLocation,
    val onDraftLocationChange: (SelectedLocation) -> Unit,
    val onMapStateChange: (MejenguerosLocationMapState) -> Unit,
    val modifier: Modifier,
)

@Composable
fun MejenguerosLocationField(
    selectedLocation: SelectedLocation?,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Ubicación del complejo",
    helperText: String = "Definí el punto exacto donde los jugadores deben llegar.",
) {
  Card(
      modifier = modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = helperText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
      ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
              text =
                  if (selectedLocation == null) "Sin ubicación seleccionada"
                  else "Ubicación seleccionada",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
              text =
                  selectedLocation?.let { "Coordenadas: ${it.toCoordinatePairText()}" }
                      ?: "Todavía no elegiste un punto en el mapa.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      MejenguerosFullWidthOutlinedButton(
          text = if (selectedLocation == null) "Seleccionar ubicación" else "Cambiar ubicación",
          onClick = onOpenPicker,
      )
    }
  }
}

@Composable
fun MejenguerosLocationPickerScreen(
    state: MejenguerosLocationPickerState,
    actions: MejenguerosLocationPickerActions,
    modifier: Modifier = Modifier,
    mapContent: @Composable (MejenguerosLocationPickerMapScope) -> Unit = { scope ->
      SideEffect { scope.onMapStateChange(MejenguerosLocationMapState.Unsupported) }
      DefaultLocationPickerMapPlaceholder(
          draftLocation = scope.draftLocation,
          mapState = MejenguerosLocationMapState.Unsupported,
          modifier = scope.modifier,
      )
    },
) {
  PlatformBackHandler(onBack = actions.onDismiss)

  Box(
      modifier =
          modifier
              .fillMaxSize()
              .semantics { paneTitle = "Location picker" }
              .testTag("location_picker_overlay")
  ) {
    mapContent(
        MejenguerosLocationPickerMapScope(
            draftLocation = state.draftLocation,
            onDraftLocationChange = actions.onDraftLocationChange,
            onMapStateChange = {},
            modifier = Modifier.fillMaxSize(),
        )
    )

    Column(
        modifier =
            Modifier.align(Alignment.TopStart)
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)
                .testTag("location_picker_top_controls"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        LocationPickerOverlayBadge(
            text = "Elegí la ubicación",
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = MaterialTheme.shapes.large,
        ) {
          TextButton(onClick = actions.onDismiss) { Text(text = "Cancelar") }
        }
      }
    }

    Column(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp)
                .testTag("location_picker_bottom_controls"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
          modifier = Modifier.fillMaxWidth(),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
          shape = MaterialTheme.shapes.large,
      ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
              text = "Punto seleccionado",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
              text = state.draftLocation.toCoordinatePairText(),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      MejenguerosFullWidthPrimaryButton(
          text = "Usar esta ubicación",
          onClick = { actions.onConfirm(state.draftLocation) },
      )
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
internal fun DefaultLocationPickerMapPlaceholder(
    draftLocation: SelectedLocation,
    mapState: MejenguerosLocationMapState,
    modifier: Modifier = Modifier,
) {
  val currentCoordinates = draftLocation.toCoordinatePairText()
  val cardModifier = modifier.fillMaxWidth()

  when (mapState) {
    MejenguerosLocationMapState.Loading -> {
      LocationPickerMapMessageCard(
          modifier = cardModifier,
          title = "Cargando mapa",
          description = "El proveedor del mapa todavía no está listo.",
      )
    }
    MejenguerosLocationMapState.Ready -> Unit
    MejenguerosLocationMapState.Unsupported -> {
      LocationPickerMapMessageCard(
          modifier = cardModifier,
          title = "Mapa no disponible",
          description =
              "El mapa no está disponible en esta plataforma por ahora. Podés continuar con las coordenadas actuales: $currentCoordinates",
      )
    }
    MejenguerosLocationMapState.Error -> {
      LocationPickerMapMessageCard(
          modifier = cardModifier,
          title = "No se pudo iniciar el mapa",
          description = "No pudimos cargar el mapa en este momento. Intentá de nuevo más tarde.",
      )
    }
  }
}

@Composable
internal fun LocationPickerMapMessageCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
  Surface(
      modifier = modifier,
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = MaterialTheme.shapes.large,
  ) {
    Box(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
      Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
internal fun LocationPickerOverlayBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
  Surface(
      modifier = modifier,
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
      shape = MaterialTheme.shapes.large,
  ) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
  }
}
