# Cognito Email And Password Auth Analysis

## Goal

Add manual user registration and password recovery using email and password while keeping Cognito as the identity provider.

The current project already uses Cognito Hosted UI with Authorization Code Flow and PKCE for Google and Microsoft. The API trusts Cognito-issued JWTs through `CognitoAuthGuard`; it does not validate Google or Microsoft tokens directly.

## Current State

### Backend API

The API currently has:

- `GET /v1/auth/me` to return the authenticated Cognito claims.
- `CognitoAuthGuard` to validate Cognito tokens.
- User synchronization through `GET /v1/users/me`.
- No endpoint that receives user passwords.
- No Cognito Identity Provider SDK dependency for signup, password reset, or confirmation commands.

This is a good baseline. The backend should continue treating Cognito as the source of truth for authentication.

### Infrastructure

The Cognito module already has several required pieces:

- `username_attributes = ["email"]`
- `auto_verified_attributes = ["email"]`
- `account_recovery_setting` with `verified_email`
- `supported_identity_providers` includes `COGNITO`
- App client uses Authorization Code Flow
- App client has no client secret, which is correct for mobile/public clients
- Password policy is already strict

The infra is close, but it should make manual email/password behavior explicit instead of relying on defaults.

### Mobile App

The KMP app currently starts Cognito Hosted UI with a forced identity provider:

- Google uses `identity_provider=Google`
- Microsoft uses `identity_provider=Microsoft`

There is no email/password option because the factory always receives an `AuthProvider` and always appends `identity_provider`.

## Baseline Approach

Use Cognito Hosted UI for email/password registration, login, and password recovery.

This means the app should add a third entry point such as `Continue with email`, but that button should open the Cognito authorization URL without the `identity_provider` query parameter. Cognito will then show the native Cognito username/password page, including sign up and forgot password links.

This is the smallest implementation because:

- The app never handles raw passwords.
- The backend does not need password endpoints.
- PKCE remains the same.
- Tokens are still Cognito JWTs, so the existing API guard continues to work.
- Email verification and password recovery stay inside Cognito.
- It avoids duplicating Cognito security rules in our API.

## User Flows

### Register With Email And Password

1. User taps `Continue with email`.
2. App opens Cognito Hosted UI without `identity_provider`.
3. User selects sign up on Cognito.
4. User enters email and password.
5. Cognito sends the email verification code.
6. User confirms the email in Cognito.
7. Cognito redirects to the app callback with an authorization code.
8. App exchanges the code with PKCE.
9. App stores tokens in secure storage.
10. App calls `GET /v1/users/me` to sync the local profile.

### Login With Email And Password

1. User taps `Continue with email`.
2. App opens Cognito Hosted UI.
3. User enters email and password in Cognito.
4. Cognito redirects to the app callback.
5. Existing token exchange and session restore flow runs.

### Forgot Password

1. User taps `Continue with email`.
2. User chooses forgot password on Cognito Hosted UI.
3. Cognito sends a recovery code to the verified email.
4. User sets a new password in Cognito.
5. User signs in normally.

For a first version, we do not need separate backend endpoints for forgot password.

## Required Changes

### Terraform

Update `app-backend/infra/modules/aws/cognito_user_pool/main.tf`:

- Add `username_configuration { case_sensitive = false }`.
- Add `admin_create_user_config { allow_admin_create_user_only = false }` so self sign-up is explicit.
- Keep `account_recovery_setting` as email-based recovery.
- Keep `COGNITO` in `supported_identity_providers`.
- Optionally add email verification templates for a cleaner user experience.

Recommended optional variables:

- `cognito_self_signup_enabled`
- `cognito_email_verification_subject`
- `cognito_email_verification_message`
- `cognito_password_recovery_subject`
- `cognito_password_recovery_message`

For production, consider SES-backed Cognito email delivery. Cognito default email is fine for development and demos but has service limits and less branding control.

### Mobile KMP

Update auth modeling:

- Add a new auth entry point for email/password.
- Do not model it as Google/Microsoft. It is the default Cognito provider.
- Make `identity_provider` optional in `CognitoOAuthRequestFactory`.

Suggested model:

```kotlin
enum class AuthProvider(val cognitoIdentityProvider: String?, val displayName: String) {
  Email(cognitoIdentityProvider = null, displayName = "Email"),
  Google(cognitoIdentityProvider = "Google", displayName = "Google"),
  Microsoft(cognitoIdentityProvider = "Microsoft", displayName = "Microsoft"),
}
```

Then append `identity_provider` only when it is not null.

Update UI:

- Add `Continue with email`.
- Keep Google and Microsoft buttons.
- Avoid wording like `Cognito`; users should see normal product language.

Update ViewModel:

- Add `signInWithEmail()`.
- Reuse the same PKCE/callback/session flow.

Update tests:

- `CognitoOAuthRequestFactory` should not append `identity_provider` for email.
- Existing Google and Microsoft tests should still assert the provider parameter.
- `AuthViewModel` should open the email login URL.
- Existing callback cold-start and one-shot tests remain valid.

### Backend API

No new backend endpoint is required for the recommended Hosted UI approach.

Keep:

- `GET /v1/auth/me`
- `GET /v1/users/me`
- Token verification through `CognitoAuthGuard`

The API should not receive passwords in this approach.

Update docs only:

- API README auth section.
- Auth decision document.
- Mobile README configuration section.

## Selected Approach: Custom App Screens

The product should let users enter email and password inside the app. The app sends those credentials directly to Cognito. The Mejengueros backend still does not receive passwords.

This gives a more familiar native app experience while keeping Cognito as the credential owner.

There are two possible variants:

- App calls Cognito directly.
- Backend receives email/password and calls Cognito.

For Mejengueros, if we need native email/password screens, the better variant is app calls Cognito directly. The backend should still avoid receiving passwords.

## Native App Email And Password Flow

In this approach, the user enters email and password inside the mobile app. The app sends those credentials directly to Cognito User Pool APIs over HTTPS. The Mejengueros backend does not receive the password.

```text
User
  -> Mobile app email/password form
  -> Cognito User Pool API
  -> Cognito tokens
  -> Mobile app secure storage
  -> Mejengueros API with Cognito bearer token
```

### Native Registration

1. User opens the register screen.
2. User enters email and password.
3. App calls Cognito `SignUp`.
4. Cognito creates an unconfirmed user and sends a verification code to email.
5. App navigates to a confirm email screen.
6. User enters the verification code.
7. App calls Cognito `ConfirmSignUp`.
8. App sends the user to login, or automatically starts login after confirmation.
9. After login, the app stores tokens in secure storage.
10. App calls `GET /v1/users/me` to sync the local application profile.

Required app screens:

- Register with email/password.
- Confirm email with verification code.
- Resend confirmation code.
- Login with email/password.

Recommended app methods:

```kotlin
interface IAuthRepository {
  suspend fun registerWithEmail(email: String, password: String)
  suspend fun confirmRegistration(email: String, code: String)
  suspend fun resendRegistrationCode(email: String)
  suspend fun signInWithEmail(email: String, password: String): AuthSession
}
```

### Native Login

1. User enters email and password.
2. App calls Cognito auth flow.
3. Cognito returns tokens when credentials are valid.
4. App decodes the ID token for display data.
5. App stores tokens in secure storage.
6. App calls backend endpoints with `Authorization: Bearer <id_token>`.

Prefer Cognito SRP auth when possible. Avoid sending passwords to the Mejengueros API.

Possible Cognito flows:

- `USER_SRP_AUTH`: preferred when the SDK/platform support is practical.
- `USER_PASSWORD_AUTH`: simpler, but the app sends the password directly to Cognito over HTTPS.

The current Terraform app client already includes:

```hcl
explicit_auth_flows = [
  "ALLOW_REFRESH_TOKEN_AUTH",
  "ALLOW_USER_SRP_AUTH",
  "ALLOW_USER_PASSWORD_AUTH"
]
```

That means the Cognito app client can support native login flows.

### Native Forgot Password

1. User taps forgot password.
2. User enters email.
3. App calls Cognito `ForgotPassword`.
4. Cognito sends a recovery code to the verified email.
5. App navigates to reset password screen.
6. User enters email, code, and new password.
7. App calls Cognito `ConfirmForgotPassword`.
8. App sends the user back to login.

Required app screens:

- Forgot password email form.
- Reset password form with code and new password.

Recommended app methods:

```kotlin
interface IAuthRepository {
  suspend fun requestPasswordReset(email: String)
  suspend fun confirmPasswordReset(email: String, code: String, newPassword: String)
}
```

### Native Auth Components

Add a Cognito native auth data source in `app-frontend/shared`:

```text
data/auth/
  CognitoNativeAuthDataSource.kt
  ICognitoNativeAuthDataSource.kt
  CognitoNativeAuthErrorMapper.kt
```

Repository ownership:

```text
AuthViewModel
  -> IAuthRepository
  -> CognitoNativeAuthDataSource
  -> Cognito User Pool API
```

Secure storage remains the same:

```text
AuthSession, id_token, access_token, refresh_token
  -> IAuthSecureStorage
  -> Android encrypted storage / iOS Keychain / JVM dev memory
```

The backend remains:

```text
Mejengueros API
  -> validates Cognito JWT
  -> syncs local user profile
```

### Native Auth Error Handling

Map Cognito errors into user-safe app messages:

- `UsernameExistsException` -> email already registered.
- `InvalidPasswordException` -> password does not meet policy.
- `CodeMismatchException` -> invalid verification code.
- `ExpiredCodeException` -> expired verification code.
- `NotAuthorizedException` -> invalid email or password.
- `UserNotConfirmedException` -> email needs confirmation.
- `TooManyRequestsException` / `LimitExceededException` -> too many attempts.

Do not show raw Cognito exception text directly to users.

For forgot password, avoid confirming whether the email exists. A safe message is:

```text
If the account exists, a recovery code was sent.
```

### Native Auth Security Rules

- Never send passwords to the Mejengueros backend.
- Never store passwords.
- Never log passwords, verification codes, OAuth codes, or tokens.
- Keep all Cognito calls over HTTPS.
- Keep tokens in secure storage only.
- Do not put client secrets in the app.
- Keep the Cognito app client public with `generate_secret = false`.
- Use Cognito password policy as the source of truth.
- Rate-limit UI retries and show cooldowns after Cognito throttling responses.
- Test app cold-start and retry flows carefully.

### Is Native Email/Password Less Secure?

It is usually less safe than Hosted UI because the app now handles password input and must avoid accidental leaks in logs, crash reports, analytics, screenshots, and state persistence.

It is still acceptable when implemented carefully because the password goes directly to Cognito, not to our backend. The main tradeoff is that we own more security-sensitive UI and error handling.

Hosted UI has the smaller risk surface. Native screens have better product control.

## Backend-Owned Email And Password Flow

This is the variant where the app sends email/password to the Mejengueros API, and the API calls Cognito.

This is not recommended unless we have a strong product or compliance reason.

Reasons to avoid it:

- The backend receives raw passwords.
- More logging and observability risk.
- More public unauthenticated endpoints.
- More abuse/rate-limit responsibility.
- More test coverage required.
- The API becomes part of the credential handling boundary.

If this variant is chosen, the backend would need to call Cognito commands:

- `SignUp`
- `ConfirmSignUp`
- `InitiateAuth`
- `ForgotPassword`
- `ConfirmForgotPassword`
- optionally `ResendConfirmationCode`

For backend-owned endpoints, the API would need:

- `POST /v1/auth/register`
- `POST /v1/auth/register/confirm`
- `POST /v1/auth/password/forgot`
- `POST /v1/auth/password/reset`
- optionally `POST /v1/auth/login`
- optionally `POST /v1/auth/register/resend-code`

This would require:

- Add `@aws-sdk/client-cognito-identity-provider`.
- Add an auth provider port in `application`.
- Add Cognito adapter in `infrastructure`.
- Add request/response DTOs in `interfaces/http`.
- Add domain/application errors that map Cognito failures safely.
- Add throttling or rate limiting for public auth endpoints.
- Add OpenAPI documentation for every auth command.
- Add unit and integration tests for each error path.

This approach gives more UI control, but it also means the product handles password input directly and must own more security-sensitive behavior.

## Error Handling Requirements If Custom Endpoints Are Added

All errors should use the existing API envelope.

Recommended public error codes:

- `AUTH_EMAIL_ALREADY_EXISTS`
- `AUTH_INVALID_CREDENTIALS`
- `AUTH_INVALID_VERIFICATION_CODE`
- `AUTH_EXPIRED_VERIFICATION_CODE`
- `AUTH_USER_NOT_CONFIRMED`
- `AUTH_PASSWORD_POLICY_FAILED`
- `AUTH_TOO_MANY_ATTEMPTS`
- `AUTH_PASSWORD_RESET_REQUIRED`

Do not leak whether an email exists during forgot password. Use a generic success response such as:

```json
{
  "success": true,
  "data": {
    "message": "If the account exists, a recovery code was sent."
  },
  "errors": [],
  "meta": {}
}
```

## Security Notes

- Do not store passwords in the app, API, logs, database, Terraform outputs, or GitHub secrets beyond Cognito-managed flows.
- Keep `prevent_user_existence_errors = "ENABLED"`.
- Keep the app client public with `generate_secret = false`.
- Keep Authorization Code Flow with PKCE.
- Use Cognito-issued ID/access tokens only.
- Never send Google or Microsoft tokens directly to the API.
- Redact `Authorization`, cookies, OAuth codes, passwords, and verification codes from logs.

## Implementation Plan

### Phase 1: Native Email Registration And Login

1. Make Cognito self sign-up explicit in Terraform.
2. Add email verification message configuration.
3. Add a native Cognito auth data source in KMP.
4. Add app methods for `SignUp`, `ConfirmSignUp`, `ResendConfirmationCode`, and `InitiateAuth`.
5. Add email/password login and registration screens.
6. Store only Cognito tokens in secure storage.
7. Keep Google and Microsoft login through the existing Hosted UI PKCE flow.

This phase should be enough for:

- email/password registration;
- email/password login;
- forgot password;
- email verification;
- normal token-based API access.

### Phase 2: Native Password Recovery

1. Add forgot password screen.
2. Call Cognito `ForgotPassword`.
3. Add reset password screen with email, code, and new password.
4. Call Cognito `ConfirmForgotPassword`.
5. Return users to login after successful reset.

### Phase 3: Hardening And UX Polish

1. Improve user-facing copy and validation.
2. Add a help link for users who do not receive verification emails.
3. Configure SES for branded Cognito emails if needed.
4. Add analytics events for auth start/success/failure without logging secrets.
5. Consider switching native login from `USER_PASSWORD_AUTH` to SRP if the KMP implementation cost is acceptable.

## Acceptance Criteria

- User can register with email/password through Cognito.
- User can verify email through Cognito.
- User can recover password through Cognito email recovery.
- User can login with email/password and receives Cognito tokens.
- Existing Google and Microsoft login continue working.
- API accepts the resulting Cognito token in `GET /v1/auth/me`.
- `GET /v1/users/me` creates or updates the local profile.
- No raw password reaches the Mejengueros API.
- Unit tests cover OAuth URL generation for email, Google, and Microsoft.
- Documentation explains that email/password is Cognito Hosted UI, not a custom password backend.

## Final Recommendation

Use native app email/password screens that call Cognito directly.

This matches the expected mobile UX while preserving the main security boundary: passwords go to Cognito, not to the Mejengueros API. The backend remains focused on token verification and application data, while Cognito owns credentials, email verification, and password reset.
