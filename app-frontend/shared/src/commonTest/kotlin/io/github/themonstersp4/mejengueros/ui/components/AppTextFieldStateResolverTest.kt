package io.github.themonstersp4.mejengueros.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class AppTextFieldStateResolverTest {

  @Test
  fun `returns disabled state before any other state`() {
    assertEquals(
        MejenguerosTextFieldChromeState.Disabled,
        resolveMejenguerosTextFieldChromeState(enabled = false, isError = true, isFocused = true),
    )
  }

  @Test
  fun `returns error state before focused state when enabled`() {
    assertEquals(
        MejenguerosTextFieldChromeState.Error,
        resolveMejenguerosTextFieldChromeState(enabled = true, isError = true, isFocused = true),
    )
  }

  @Test
  fun `returns focused state when enabled and not error`() {
    assertEquals(
        MejenguerosTextFieldChromeState.Focused,
        resolveMejenguerosTextFieldChromeState(enabled = true, isError = false, isFocused = true),
    )
  }

  @Test
  fun `returns unfocused state when field is idle`() {
    assertEquals(
        MejenguerosTextFieldChromeState.Unfocused,
        resolveMejenguerosTextFieldChromeState(enabled = true, isError = false, isFocused = false),
    )
  }
}
