package io.github.themonstersp4.mejengueros.monitoring

class JvmConsoleErrorReporter : ErrorReporter {
  override fun reportRecoverableFailure(name: String, attributes: Map<String, String>) {
    System.err.println(formatRecoverableFailureMessage(name, attributes))
  }
}
