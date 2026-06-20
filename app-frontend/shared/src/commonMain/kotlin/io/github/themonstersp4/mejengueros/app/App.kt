package io.github.themonstersp4.mejengueros.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.themonstersp4.mejengueros.navigation.AppNavHost
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme

@Composable
@Preview
fun App() {
  MejenguerosTheme {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      AppNavHost()
    }
  }
}
