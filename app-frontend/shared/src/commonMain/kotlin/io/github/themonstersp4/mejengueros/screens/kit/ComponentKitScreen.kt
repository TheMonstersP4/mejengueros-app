package io.github.themonstersp4.mejengueros.screens.kit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosCourtCard
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListGroup
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosListItem
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosReservationSummaryBar
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateContent
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStateVariant
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPill
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosStatusPillStyle
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosThumbnail
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTicketSummary
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosTicketSummaryRow

@Composable
fun ComponentKitScreen(
    contentPadding: PaddingValues,
    onOpenAvailabilitySelectors: () -> Unit,
    modifier: Modifier = Modifier,
) {
  LazyColumn(
      modifier = modifier.fillMaxSize(),
      contentPadding =
          PaddingValues(
              start = 20.dp,
              top = contentPadding.calculateTopPadding() + 20.dp,
              end = 20.dp,
              bottom = contentPadding.calculateBottomPadding() + 20.dp,
          ),
      verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    item { ComponentKitHeader() }
    item { SelectorDemoSection(onOpenAvailabilitySelectors = onOpenAvailabilitySelectors) }
    item { StatusPillsSection() }
    item { ThumbnailSection() }
    item { ListItemsSection() }
    item { CourtCardSection() }
    item { ReservationStatesSection() }
    item { ReservationSummarySection() }
  }
}

@Composable
private fun ComponentKitHeader() {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
        text = "Kit",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text =
            "Área temporal de desarrollo para validar visualmente los componentes reutilizables de Mejengueros.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun SelectorDemoSection(
    onOpenAvailabilitySelectors: () -> Unit,
) {
  ComponentKitSection(
      title = "Selectores",
      description = "Demos de fecha, día, horario y slots creados para disponibilidad y reservas.",
  ) {
    MejenguerosFullWidthOutlinedButton(
        text = "Ver selectores de disponibilidad",
        onClick = onOpenAvailabilitySelectors,
    )
  }
}

@Composable
private fun StatusPillsSection() {
  ComponentKitSection(
      title = "Estados",
      description = "Badges reutilizables para disponibilidad, gestión, reservas y confirmaciones.",
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf(
              "Disponible" to MejenguerosStatusPillStyle.Primary,
              "Activa" to MejenguerosStatusPillStyle.Neutral,
              "Pendiente" to MejenguerosStatusPillStyle.Subtle,
              "Cancelada" to MejenguerosStatusPillStyle.Error,
          )
          .forEach { (text, style) -> MejenguerosStatusPill(text = text, style = style) }
    }
  }
}

@Composable
private fun ThumbnailSection() {
  ComponentKitSection(
      title = "Miniaturas",
      description = "Estados con imagen remota y placeholder para tarjetas o filas.",
  ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      MejenguerosThumbnail(
          imageUrl = null,
          contentDescription = "Cancha sin imagen",
      )
      MejenguerosThumbnail(
          imageUrl = "https://images.unsplash.com/photo-1556056504-5c7696c4c28d",
          contentDescription = "Cancha de fútbol demo",
      )
    }
  }
}

@Composable
private fun ListItemsSection() {
  ComponentKitSection(
      title = "Filas",
      description = "List items base con leading, supporting text, trailing y separadores.",
  ) {
    MejenguerosListGroup {
      MejenguerosListItem(
          title = "Cancha 5 vs 5",
          supportingText = "Sintética · Iluminación nocturna",
          leading = { ComponentInitialsBadge(text = "C5") },
          trailing = {
            MejenguerosStatusPill(
                text = "Activa",
                style = MejenguerosStatusPillStyle.Primary,
            )
          },
          showDivider = true,
      )
      MejenguerosListItem(
          title = "Reserva de hoy",
          supportingText = "20:00 · Complejo La Sabana",
          leading = { ComponentInitialsBadge(text = "R") },
          trailing = { Text(text = "›", style = MaterialTheme.typography.titleLarge) },
          showDivider = true,
      )
      MejenguerosListItem(
          title = "Nueva notificación",
          supportingText = "Un jugador confirmó asistencia.",
          leading = { ComponentInitialsBadge(text = "N") },
          trailing = {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                content = {},
            )
          },
      )
    }
  }
}

@Composable
private fun CourtCardSection() {
  ComponentKitSection(
      title = "Tarjetas",
      description = "Composición inicial para catálogo de canchas o complejos.",
  ) {
    MejenguerosCourtCard(
        title = "Complejo Deportivo La Sabana",
        location = "San José · 1.4 km",
        imageUrl = null,
        metadata = listOf("Sintética", "5 vs 5", "Techada"),
        statusText = "Disponible hoy",
    )
  }
}

@Composable
private fun ReservationStatesSection() {
  ComponentKitSection(
      title = "Estados de reserva",
      description = "Resultados reutilizables para éxito, error, vacío y backend pendiente.",
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      MejenguerosStateContent(
          title = "Reserva confirmada",
          description =
              "Tu cancha quedó bloqueada por una hora. Te enviaremos recordatorios antes del partido.",
          variant = MejenguerosStateVariant.Success,
          body = { DemoReservationTicket() },
          actions = {
            MejenguerosFullWidthPrimaryButton(
                text = "Ver mis reservas",
                onClick = {},
            )
            MejenguerosFullWidthOutlinedButton(
                text = "Volver al catálogo",
                onClick = {},
            )
          },
      )
      MejenguerosStateContent(
          title = "Horario no disponible",
          description =
              "Alguien reservó este horario antes de confirmar. Elige otro espacio disponible.",
          variant = MejenguerosStateVariant.Error,
          actions = {
            MejenguerosFullWidthPrimaryButton(
                text = "Elegir otro horario",
                onClick = {},
            )
          },
      )
      MejenguerosStateContent(
          title = "Sin resultados todavía",
          description = "Cuando existan reservas o reseñas pendientes, aparecerán en este espacio.",
          variant = MejenguerosStateVariant.Empty,
      )
      MejenguerosStateContent(
          title = "Conexión pendiente",
          description =
              "Este flujo visual está listo, pero la acción real depende de integrar el backend.",
          variant = MejenguerosStateVariant.Pending,
          actions = {
            MejenguerosFullWidthOutlinedButton(
                text = "Entendido",
                onClick = {},
                enabled = false,
            )
          },
      )
    }
  }
}

@Composable
private fun ReservationSummarySection() {
  ComponentKitSection(
      title = "Resumen de reserva",
      description = "Ticket de confirmación y barra inferior para revisar antes de confirmar.",
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      DemoReservationTicket()
      MejenguerosReservationSummaryBar(
          summary = "Cancha 5 vs 5 · Hoy 20:00",
          supportingText = "Complejo Deportivo La Sabana · 1 hora",
          actionText = "Confirmar reserva",
          onActionClick = {},
      )
    }
  }
}

@Composable
private fun DemoReservationTicket() {
  MejenguerosTicketSummary(
      title = "Detalle de reserva",
      rows =
          listOf(
              MejenguerosTicketSummaryRow(
                  label = "Cancha",
                  value = "Cancha 5 vs 5",
                  supportingText = "Complejo Deportivo La Sabana",
              ),
              MejenguerosTicketSummaryRow(
                  label = "Fecha",
                  value = "Hoy, 16 de junio",
              ),
              MejenguerosTicketSummaryRow(
                  label = "Horario",
                  value = "20:00 – 21:00",
              ),
          ),
  )
}

@Composable
private fun ComponentKitSection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      content()
    }
  }
}

@Composable
private fun ComponentInitialsBadge(
    text: String,
) {
  Surface(
      modifier = Modifier.size(40.dp),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
          text = text,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          textAlign = TextAlign.Center,
      )
    }
  }
}
