package io.github.themonstersp4.mejengueros.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

internal enum class MejenguerosTextFieldChromeState {
  Disabled,
  Error,
  Focused,
  Unfocused,
}

internal fun resolveMejenguerosTextFieldChromeState(
    enabled: Boolean,
    isError: Boolean,
    isFocused: Boolean,
): MejenguerosTextFieldChromeState =
    when {
      !enabled -> MejenguerosTextFieldChromeState.Disabled
      isError -> MejenguerosTextFieldChromeState.Error
      isFocused -> MejenguerosTextFieldChromeState.Focused
      else -> MejenguerosTextFieldChromeState.Unfocused
    }

private data class MejenguerosTextFieldColors(
    val textColor: Color,
    val borderColor: Color,
    val labelColor: Color,
    val containerColor: Color,
    val cursorColor: Color,
    val supportingTextColor: Color,
)

private val TextFieldHorizontalPadding = 16.dp
private val TextFieldVerticalPadding = 12.dp
private val TextFieldLabelSpacing = 6.dp
private val TextFieldSupportingTextTopPadding = 4.dp
private val TextFieldTrailingIconSpacing = 12.dp
private val TextFieldTrailingSlotSize = 48.dp
private val TextFieldBorderWidth = 1.dp

@Composable
fun MejenguerosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    showLabel: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
  var isFocused by remember { mutableStateOf(false) }
  val chromeState =
      resolveMejenguerosTextFieldChromeState(
          enabled = enabled,
          isError = isError,
          isFocused = isFocused,
      )
  val colors = mejenguerosCustomTextFieldColors(chromeState)
  val interactionSource = remember { MutableInteractionSource() }
  val labelInteractionSource = remember { MutableInteractionSource() }
  val containerInteractionSource = remember { MutableInteractionSource() }
  val focusRequester = remember { FocusRequester() }
  val containerModifier =
      if (enabled && !readOnly) {
        Modifier.clickable(
            interactionSource = containerInteractionSource,
            indication = null,
        ) {
          focusRequester.requestFocus()
        }
      } else {
        Modifier
      }
  val labelModifier =
      if (enabled && !readOnly) {
        Modifier.clickable(
            interactionSource = labelInteractionSource,
            indication = null,
        ) {
          focusRequester.requestFocus()
        }
      } else {
        Modifier
      }
  val contentEndPadding =
      if (trailingIcon != null) {
        TextFieldHorizontalPadding + TextFieldTrailingIconSpacing + TextFieldTrailingSlotSize
      } else {
        TextFieldHorizontalPadding
      }

  Column(modifier = modifier.fillMaxWidth()) {
    Surface(
        modifier =
            Modifier.fillMaxWidth().then(containerModifier).testTag("$label text field container"),
        shape = MaterialTheme.shapes.medium,
        color = colors.containerColor,
        border = BorderStroke(TextFieldBorderWidth, colors.borderColor),
    ) {
      Layout(
          modifier = Modifier.fillMaxWidth(),
          content = {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            start = TextFieldHorizontalPadding,
                            top = TextFieldVerticalPadding,
                            end = contentEndPadding,
                            bottom = TextFieldVerticalPadding,
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(if (showLabel) TextFieldLabelSpacing else 0.dp),
            ) {
              if (showLabel) {
                Text(
                    text = label,
                    modifier = labelModifier,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.labelColor,
                )
              }
              Box(modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier =
                        Modifier.fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused }
                            .semantics {
                              contentDescription = label
                              if (isError && !supportingText.isNullOrBlank()) {
                                error(supportingText)
                              }
                            },
                    enabled = enabled,
                    readOnly = readOnly,
                    singleLine = singleLine,
                    minLines = minLines,
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                    visualTransformation = visualTransformation,
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = colors.textColor,
                        ),
                    cursorBrush = SolidColor(colors.cursorColor),
                    interactionSource = interactionSource,
                    decorationBox = { innerTextField ->
                      if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.labelColor,
                        )
                      }
                      innerTextField()
                    },
                )
              }
            }

            if (trailingIcon != null) {
              Box(
                  modifier = Modifier.size(TextFieldTrailingSlotSize),
                  contentAlignment = Alignment.Center,
              ) {
                CompositionLocalProvider(LocalContentColor provides colors.textColor) {
                  trailingIcon()
                }
              }
            }
          },
          measurePolicy = { measurables, constraints ->
            val contentPlaceable = measurables.first().measure(constraints)
            val trailingSlotSize = TextFieldTrailingSlotSize.roundToPx()
            val trailingPlaceable =
                measurables
                    .getOrNull(1)
                    ?.measure(
                        constraints.copy(
                            minWidth = trailingSlotSize,
                            maxWidth = trailingSlotSize,
                            minHeight = trailingSlotSize,
                            maxHeight = trailingSlotSize,
                        )
                    )

            layout(contentPlaceable.width, contentPlaceable.height) {
              contentPlaceable.placeRelative(0, 0)

              if (trailingPlaceable != null) {
                val trailingEndPadding = TextFieldHorizontalPadding.roundToPx()
                val trailingX =
                    contentPlaceable.width - trailingEndPadding - trailingPlaceable.width
                val trailingY =
                    if (singleLine) {
                      (contentPlaceable.height - trailingPlaceable.height) / 2
                    } else {
                      TextFieldVerticalPadding.roundToPx()
                    }
                trailingPlaceable.placeRelative(trailingX, trailingY)
              }
            }
          },
      )
    }

    if (supportingText != null) {
      Text(
          text = supportingText,
          modifier =
              Modifier.padding(
                  start = TextFieldHorizontalPadding,
                  top = TextFieldSupportingTextTopPadding,
              ),
          style = MaterialTheme.typography.bodySmall,
          color = colors.supportingTextColor,
      )
    }
  }
}

@Composable
private fun mejenguerosOutlinedTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorLabelColor = MaterialTheme.colorScheme.error,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        cursorColor = MaterialTheme.colorScheme.primary,
        errorCursorColor = MaterialTheme.colorScheme.error,
        focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorSupportingTextColor = MaterialTheme.colorScheme.error,
    )

@Composable
private fun mejenguerosCustomTextFieldColors(
    chromeState: MejenguerosTextFieldChromeState
): MejenguerosTextFieldColors {
  val colorScheme = MaterialTheme.colorScheme

  return when (chromeState) {
    MejenguerosTextFieldChromeState.Disabled ->
        MejenguerosTextFieldColors(
            textColor = colorScheme.onSurfaceVariant,
            borderColor = colorScheme.outlineVariant,
            labelColor = colorScheme.onSurfaceVariant,
            containerColor = colorScheme.surfaceContainerHigh,
            cursorColor = colorScheme.onSurfaceVariant,
            supportingTextColor = colorScheme.onSurfaceVariant,
        )
    MejenguerosTextFieldChromeState.Error ->
        MejenguerosTextFieldColors(
            textColor = colorScheme.onSurface,
            borderColor = colorScheme.error,
            labelColor = colorScheme.error,
            containerColor = colorScheme.surfaceContainerHighest,
            cursorColor = colorScheme.error,
            supportingTextColor = colorScheme.error,
        )
    MejenguerosTextFieldChromeState.Focused ->
        MejenguerosTextFieldColors(
            textColor = colorScheme.onSurface,
            borderColor = colorScheme.primary,
            labelColor = colorScheme.primary,
            containerColor = colorScheme.surfaceContainerHighest,
            cursorColor = colorScheme.primary,
            supportingTextColor = colorScheme.onSurfaceVariant,
        )
    MejenguerosTextFieldChromeState.Unfocused ->
        MejenguerosTextFieldColors(
            textColor = colorScheme.onSurface,
            borderColor = colorScheme.outlineVariant,
            labelColor = colorScheme.onSurfaceVariant,
            containerColor = colorScheme.surfaceContainerHighest,
            cursorColor = colorScheme.primary,
            supportingTextColor = colorScheme.onSurfaceVariant,
        )
  }
}

@Composable
fun MejenguerosTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    minLines: Int = 3,
    maxLines: Int = 5,
    showLabel: Boolean = true,
) {
  MejenguerosTextField(
      value = value,
      onValueChange = onValueChange,
      label = label,
      modifier = modifier,
      placeholder = placeholder,
      enabled = enabled,
      isError = isError,
      supportingText = supportingText,
      singleLine = false,
      minLines = minLines,
      maxLines = maxLines,
      showLabel = showLabel,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MejenguerosSelectField(
    value: String,
    label: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }

  ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { if (enabled) expanded = it },
      modifier = modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier =
            Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        enabled = enabled,
        readOnly = true,
        singleLine = true,
        isError = isError,
        label = { Text(label) },
        supportingText = supportingText?.let { message -> ({ Text(message) }) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        shape = MaterialTheme.shapes.medium,
        colors = mejenguerosOutlinedTextFieldColors(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
            text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
            onClick = {
              onOptionSelected(option)
              expanded = false
            },
            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
        )
      }
    }
  }
}

@Composable
fun MejenguerosEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
) {
  MejenguerosTextField(
      value = value,
      onValueChange = onValueChange,
      label = "Correo electrónico",
      modifier = modifier,
      enabled = enabled,
      isError = isError,
      supportingText = supportingText,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
  )
}

fun Modifier.clearFocusOnTap(focusManager: FocusManager? = null): Modifier = composed {
  val currentFocusManager = focusManager ?: LocalFocusManager.current

  pointerInput(currentFocusManager) {
    awaitEachGesture {
      awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
      val up = waitForUpOrCancellation(pass = PointerEventPass.Final)

      if (up != null && !up.isConsumed) {
        currentFocusManager.clearFocus()
      }
    }
  }
}

@Composable
fun MejenguerosPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Contraseña",
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
) {
  var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
  val visibilityDescription = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
  val visualTransformation =
      remember(isPasswordVisible) {
        if (isPasswordVisible) {
          VisualTransformation.None
        } else {
          PasswordVisualTransformation()
        }
      }

  MejenguerosTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier,
      label = label,
      enabled = enabled,
      isError = isError,
      supportingText = supportingText,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
      visualTransformation = visualTransformation,
      trailingIcon = {
        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }, enabled = enabled) {
          VisibilityIcon(
              visible = isPasswordVisible,
              contentDescription = visibilityDescription,
          )
        }
      },
  )
}
