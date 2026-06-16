# Cognito User Pool Module

Creates a Cognito user pool with hosted UI, an app client, an admin group, and optional Google and Microsoft login.

## Resources

- `aws_cognito_user_pool.user_pool`
- `aws_cognito_identity_provider.google`
- `aws_cognito_identity_provider.microsoft`
- `aws_cognito_user_pool_client.app_client`
- `aws_cognito_user_pool_domain.domain`
- `aws_cognito_user_group.admin`
- `aws_cognito_user.admin`
- `aws_cognito_user_in_group.admin`

## Inputs

| Name | Description |
| --- | --- |
| `name_prefix` | Prefix used for Cognito resource names. |
| `domain_prefix` | Cognito hosted UI domain prefix. |
| `mfa_configuration` | MFA mode. |
| `password_minimum_length` | Minimum password length. |
| `callback_urls` | Allowed OAuth callback URLs. |
| `logout_urls` | Allowed OAuth logout URLs. |
| `admin_users` | Optional bootstrap admin users. |
| `google_enabled` | Enables Google login. |
| `google_client_id` | Google OAuth client ID. |
| `google_client_secret` | Google OAuth client secret. |
| `microsoft_enabled` | Enables Microsoft login through OIDC. |
| `microsoft_tenant_id` | Microsoft authority tenant value. Use `9188040d-6c67-4c5b-b112-36a304b66dad` for personal Outlook, Hotmail, Live, and Skype accounts. |
| `microsoft_client_id` | Microsoft OAuth client ID. |
| `microsoft_client_secret` | Microsoft OAuth client secret. |
| `microsoft_provider_name` | Cognito provider name for Microsoft. |

## Outputs

| Name | Description |
| --- | --- |
| `user_pool_id` | Cognito user pool ID. |
| `user_pool_arn` | Cognito user pool ARN. |
| `user_pool_client_id` | Cognito app client ID. |
| `domain` | Cognito domain prefix. |
| `domain_url` | Cognito hosted UI base URL. |
| `identity_provider_redirect_url` | Redirect URL required by external identity providers such as Google and Microsoft Entra. |
| `admin_group_name` | Admin group name. |
