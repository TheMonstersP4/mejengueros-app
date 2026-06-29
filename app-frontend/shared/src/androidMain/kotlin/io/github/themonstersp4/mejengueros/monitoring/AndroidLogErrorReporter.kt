package io.github.themonstersp4.mejengueros.monitoring

import android.util.Log

class AndroidLogErrorReporter : ErrorReporter {
  override fun reportRecoverableFailure(name: String, attributes: Map<String, String>) {
    Log.e(LOG_TAG, formatRecoverableFailureMessage(name, attributes))
  }

  private companion object {
    const val LOG_TAG = "MejenguerosErrorReporter"
  }
}
