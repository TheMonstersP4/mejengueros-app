output "client_id" {
  description = "Microsoft Entra application client ID."
  value       = azuread_application.oauth_client.client_id
}

output "object_id" {
  description = "Microsoft Entra application object ID."
  value       = azuread_application.oauth_client.object_id
}

output "client_secret" {
  description = "Generated Microsoft Entra client secret."
  value       = azuread_application_password.oauth_client_secret.value
  sensitive   = true
}

output "redirect_uris" {
  description = "Registered OAuth redirect URIs."
  value       = var.redirect_uris
}
