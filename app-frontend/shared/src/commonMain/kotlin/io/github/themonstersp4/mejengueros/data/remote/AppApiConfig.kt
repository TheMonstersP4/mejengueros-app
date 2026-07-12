package io.github.themonstersp4.mejengueros.data.remote

data class AppApiConfig(
    val baseUrl: String,
    val websocketUrl: String,
)

val defaultAppApiConfig =
    AppApiConfig(
        baseUrl = "http://10.0.2.2:3000",
        websocketUrl = "wss://dilk66l4f1.execute-api.us-east-2.amazonaws.com/dev",
    )
