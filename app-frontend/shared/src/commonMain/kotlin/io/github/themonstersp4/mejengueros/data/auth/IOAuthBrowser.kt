package io.github.themonstersp4.mejengueros.data.auth

interface IOAuthBrowser {
  suspend fun open(url: String)
}
