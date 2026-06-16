# Identity Provider Automation

## Cognito

Cognito is the identity broker for the API.

```text
Google or Microsoft -> Cognito Hosted UI -> Cognito JWT -> API
```

The API should validate Cognito-issued tokens, not Google or Microsoft tokens directly.

## Microsoft Entra

Microsoft Entra app registrations are automated from the root Terraform stack
when `azuread_enabled = true`.

Terraform creates:

```text
azuread_application
azuread_application_password
```

The redirect URI is derived before resources are created:

```text
https://<project>-<env>-auth.auth.<aws_region>.amazoncognito.com/oauth2/idpresponse
```

The generated Entra `client_id` and `client_secret` are passed directly into
the Cognito Microsoft identity provider. No manual Microsoft client secret is
needed when `azuread_enabled = true`.

Keep `azuread_enabled = false` when Microsoft login is not needed. This root
stack does not accept manually managed Microsoft client secrets; use the AzureAD
module so the Entra app registration and Cognito provider stay in sync.

Enabling the AzureAD provider in the root stack requires Microsoft Entra
permissions such as application registration write access.

## Google

Do not model the normal Google social login OAuth client as a generic Terraform module yet.

The Google provider has OAuth-related resources for Google Cloud access and Workforce Identity Federation, but that is not the same as the standard external "Sign in with Google" OAuth web client used by Cognito federation.

For now:

1. Create the Google OAuth web client in Google Cloud.
2. Register the Cognito `cognito_identity_provider_redirect_url`.
3. Pass `google_client_id` and `google_client_secret` into Terraform from a secret workflow.

This avoids a fragile provider setup that looks automated but does not manage the right Google Auth Platform object.
