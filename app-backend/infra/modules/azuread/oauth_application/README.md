# AzureAD OAuth Application Module

Creates a Microsoft Entra app registration and client secret for Cognito federation.

Use the Cognito `identity_provider_redirect_url` output as the redirect URI.

## Resources

- `azuread_application.oauth_client`
- `azuread_application_password.oauth_client_secret`

The app requests access token version 2 so it can support personal Microsoft accounts when the sign-in audience includes them.

## Inputs

| Name | Description |
| --- | --- |
| `display_name` | Microsoft Entra application display name. |
| `sign_in_audience` | Supported account audience for the app registration. |
| `redirect_uris` | OAuth redirect URIs registered in Microsoft Entra. |
| `homepage_url` | Optional application homepage URL. |
| `logout_url` | Optional front-channel logout URL. |
| `owner_object_ids` | Optional Microsoft Entra object IDs that own the application. |
| `client_secret_display_name` | Display name for the generated client secret. |
| `client_secret_end_date` | Client secret expiration date in RFC3339 format. |

## Outputs

| Name | Description |
| --- | --- |
| `client_id` | Microsoft Entra application client ID. |
| `object_id` | Microsoft Entra application object ID. |
| `client_secret` | Generated Microsoft Entra client secret. |
| `redirect_uris` | Registered OAuth redirect URIs. |
