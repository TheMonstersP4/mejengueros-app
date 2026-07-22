package io.github.themonstersp4.mejengueros.data.remote

data class AppApiConfig(
    val baseUrl: String,
    val websocketUrl: String,
)

val defaultAppApiConfig =
    AppApiConfig(
        baseUrl = "https://85u7xyr1p9.execute-api.us-east-2.amazonaws.com",
        websocketUrl = "wss://dilk66l4f1.execute-api.us-east-2.amazonaws.com/dev",
    )
