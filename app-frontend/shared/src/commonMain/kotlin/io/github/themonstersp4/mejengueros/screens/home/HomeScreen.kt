package io.github.themonstersp4.mejengueros.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.themonstersp4.mejengueros.navigation.OwnerCourtAvailabilityEntrypoint
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthOutlinedButton
import io.github.themonstersp4.mejengueros.ui.components.MejenguerosFullWidthPrimaryButton

@Composable
fun HomeScreen(
    username: String,
    contentPadding: PaddingValues,
    onCreateComplex: () -> Unit,
    ownerAvailabilityEntrypoint: OwnerCourtAvailabilityEntrypoint? = null,
    onOpenOwnerAvailabilityEntrypoint: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = "Home",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Continuing as $username",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "This authenticated area will host Mejengueros features.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    MejenguerosFullWidthPrimaryButton(
        text = "Crear complejo y primera cancha",
        onClick = onCreateComplex,
        modifier = Modifier.fillMaxWidth().testTag("home_create_complex_button"),
    )
    if (ownerAvailabilityEntrypoint != null) {
      Spacer(modifier = Modifier.height(16.dp))
      Text(
          text = "Última cancha creada",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().testTag("home_owner_availability_title"),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
          text =
              "${ownerAvailabilityEntrypoint.courtName} · ${ownerAvailabilityEntrypoint.complexName}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().testTag("home_owner_availability_summary"),
      )
      Spacer(modifier = Modifier.height(12.dp))
      MejenguerosFullWidthOutlinedButton(
          text = "Configurar disponibilidad",
          onClick = onOpenOwnerAvailabilityEntrypoint,
          modifier = Modifier.fillMaxWidth().testTag("home_owner_availability_button"),
      )
    }
  }
}
