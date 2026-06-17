package io.github.themonstersp4.mejengueros

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.themonstersp4.mejengueros.app.App
import io.github.themonstersp4.mejengueros.data.auth.handleOAuthCallback

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    handleIntent(intent)

    setContent { App() }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    intent?.data?.takeIf { it.isAuthCallback() }?.toString()?.let(::handleOAuthCallback)
  }

  private fun Uri.isAuthCallback(): Boolean =
      scheme == "com.themonsters.mejengueros" && host == "auth" && path == "/callback"
}

@Preview
@Composable
fun AppAndroidPreview() {
  App()
}
