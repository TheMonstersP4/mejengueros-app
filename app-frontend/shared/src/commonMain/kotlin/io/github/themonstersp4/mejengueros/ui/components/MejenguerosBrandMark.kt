package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.github.themonstersp4.mejengueros.generated.resources.Res
import io.github.themonstersp4.mejengueros.generated.resources.mejengueros_brand_mark
import org.jetbrains.compose.resources.painterResource

@Composable
fun MejenguerosBrandMark(modifier: Modifier = Modifier) {
  Image(
      painter = painterResource(Res.drawable.mejengueros_brand_mark),
      contentDescription = "Marca de Mejengueros",
      modifier = modifier,
      contentScale = ContentScale.Fit,
  )
}
