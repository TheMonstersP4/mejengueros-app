package io.github.themonstersp4.mejengueros.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.themonstersp4.mejengueros.navigation.AppNavHost
import io.github.themonstersp4.mejengueros.theme.MejenguerosTheme

@Composable
@Preview
fun App() {
  MejenguerosTheme { AppNavHost() }
}
