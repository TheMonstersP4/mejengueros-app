package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MejenguerosOtpCodeField(
    code: String,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    supportingText: String? = null,
) {
  val sanitizedCode = code.filter { it.isDigit() }.take(length)
  val activeIndex = sanitizedCode.length.coerceAtMost(length - 1)
  val borderColor =
      if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      repeat(length) { index ->
        val value = sanitizedCode.getOrNull(index)?.toString().orEmpty()
        val isActive = index == activeIndex && sanitizedCode.length < length && !isError
        val currentBorderColor =
            when {
              isError -> MaterialTheme.colorScheme.error
              isActive -> MaterialTheme.colorScheme.primary
              else -> borderColor
            }

        Surface(
            modifier =
                Modifier.weight(1f)
                    .height(52.dp)
                    .border(
                        width = 1.dp,
                        color = currentBorderColor,
                        shape = RoundedCornerShape(12.dp),
                    ),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
          }
        }
      }
    }

    supportingText?.let { message ->
      Text(
          text = message,
          style = MaterialTheme.typography.bodySmall,
          color =
              if (isError) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
