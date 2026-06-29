package io.github.themonstersp4.mejengueros.monitoring

import platform.Foundation.NSLog

class IosSystemErrorReporter : ErrorReporter {
  override fun reportRecoverableFailure(name: String, attributes: Map<String, String>) {
    NSLog("%@", formatRecoverableFailureMessage(name, attributes))
  }
}
