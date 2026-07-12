package io.github.themonstersp4.mejengueros.data.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class AuthSessionExpirationReason {
  MissingToken,
  UnauthorizedResponse,
  ForbiddenResponse,
}

data class AuthSessionExpirationEvent(
    val reason: AuthSessionExpirationReason,
    val message: String = "Tu sesion expiro. Inicia sesion de nuevo.",
)

class AuthSessionExpirationNotifier {
  private val _events = MutableSharedFlow<AuthSessionExpirationEvent>(extraBufferCapacity = 1)
  val events: SharedFlow<AuthSessionExpirationEvent> = _events.asSharedFlow()

  fun notify(reason: AuthSessionExpirationReason) {
    _events.tryEmit(AuthSessionExpirationEvent(reason = reason))
  }
}
