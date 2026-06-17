package io.github.themonstersp4.mejengueros.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri

class AndroidOAuthBrowser(private val context: Context) : IOAuthBrowser {
  override suspend fun open(url: String) {
    val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    context.startActivity(intent)
  }
}
