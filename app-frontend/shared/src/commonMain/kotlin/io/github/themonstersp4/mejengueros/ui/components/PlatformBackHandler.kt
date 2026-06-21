package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.runtime.Composable

@Composable expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
