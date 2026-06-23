package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MejenguerosOtpCodeField(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    label: String = "Código de verificación",
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
) {
  val sanitizedCode = code.filter { it.isDigit() }.take(length)
  val focusRequester = remember { FocusRequester() }
  val interactionSource = remember { MutableInteractionSource() }

  Column(
      modifier =
          modifier.clickable(
              interactionSource = interactionSource,
              indication = null,
              enabled = enabled,
          ) {
            focusRequester.requestFocus()
          },
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    BasicTextField(
        value = sanitizedCode,
        onValueChange = { onCodeChange(it.filter(Char::isDigit).take(length)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        modifier =
            Modifier.fillMaxWidth().focusRequester(focusRequester).semantics {
              contentDescription = label
            },
        decorationBox = { innerTextField ->
          Box {
            innerTextField()
            OtpCodeBoxes(
                code = sanitizedCode,
                length = length,
                isError = isError,
            )
          }
        },
    )

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

@Composable
private fun OtpCodeBoxes(
    code: String,
    length: Int,
    isError: Boolean,
) {
  val activeIndex = code.length.coerceAtMost(length - 1)
  val borderColor =
      if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant

  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(length) { index ->
      val value = code.getOrNull(index)?.toString().orEmpty()
      val isActive = index == activeIndex && code.length < length && !isError
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
}
