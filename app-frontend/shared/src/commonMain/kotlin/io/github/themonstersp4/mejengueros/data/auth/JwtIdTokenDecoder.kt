package io.github.themonstersp4.mejengueros.data.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JwtIdTokenDecoder(private val json: Json) {
  fun decode(idToken: String): CognitoIdTokenClaims {
    val payload = idToken.split(".").getOrNull(1) ?: error("Invalid Cognito id token.")
    val claims = json.parseToJsonElement(Base64Url.decode(payload).decodeToString()).jsonObject
    return CognitoIdTokenClaims(
        sub = claims.requiredString("sub"),
        email = claims.requiredString("email"),
        displayName = claims.optionalString("name") ?: claims.optionalString("given_name"),
        provider = claims.providerName(),
        expiresAtEpochSeconds = claims.requiredLong("exp"),
    )
  }

  private fun JsonObject.requiredString(key: String): String =
      optionalString(key) ?: error("Missing required token claim: $key.")

  private fun JsonObject.requiredLong(key: String): Long =
      this[key]?.jsonPrimitive?.content?.toLongOrNull()
          ?: error("Missing required token claim: $key.")

  private fun JsonObject.optionalString(key: String): String? =
      this[key]?.jsonPrimitive?.content?.takeIf(String::isNotBlank)

  private fun JsonObject.providerName(): String? {
    val identities = this["identities"] ?: return optionalString("cognito:username")
    val identityArray =
        when (identities) {
          is JsonArray -> identities
          else -> json.parseToJsonElement(identities.jsonPrimitive.content) as? JsonArray
        } ?: return optionalString("cognito:username")
    return identityArray.firstOrNull()?.jsonObject?.get("providerName")?.jsonPrimitive?.content
  }
}
