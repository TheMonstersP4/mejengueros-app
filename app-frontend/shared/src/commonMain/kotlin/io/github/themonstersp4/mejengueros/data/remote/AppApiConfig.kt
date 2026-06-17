package io.github.themonstersp4.mejengueros.data.remote

data class AppApiConfig(val baseUrl: String)

val defaultAppApiConfig =
    AppApiConfig(baseUrl = "https://85u7xyr1p9.execute-api.us-east-2.amazonaws.com")
