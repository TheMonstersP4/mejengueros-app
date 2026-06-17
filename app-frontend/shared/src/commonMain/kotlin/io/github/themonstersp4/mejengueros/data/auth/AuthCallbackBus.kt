package io.github.themonstersp4.mejengueros.data.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object AuthCallbackBus {
  private val _callbackUrls = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
  val callbackUrls: SharedFlow<String> = _callbackUrls
  private var latestCallbackUrl: String? = null

  fun publish(callbackUrl: String) {
    latestCallbackUrl = callbackUrl
    _callbackUrls.tryEmit(callbackUrl)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  fun markConsumed(callbackUrl: String) {
    if (latestCallbackUrl == callbackUrl) {
      latestCallbackUrl = null
      _callbackUrls.resetReplayCache()
    }
  }
}

fun handleOAuthCallback(url: String) {
  AuthCallbackBus.publish(url)
}
