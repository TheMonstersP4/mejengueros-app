# Auth Decision: Cognito Hosted UI with PKCE

## Context

Mejengueros uses social login with Google and Microsoft through Amazon Cognito. The mobile app starts the login flow, Cognito redirects the user to the selected identity provider, and the app receives an OAuth callback with an authorization code.

The app uses Authorization Code Flow with PKCE. It does not embed provider login forms, does not store OAuth client secrets, and does not collect user passwords.

## Decision

Use Cognito Hosted UI as the authentication entry point for Google and Microsoft login.

The mobile app may show a friendly entry screen, such as `Continue with Google` or `Continue with Microsoft`, but the credential step must happen in Cognito, Google, Microsoft, or the system browser flow.

## Why This Is Safer

### The app never sees passwords

If login happens inside our app, the app becomes responsible for handling credentials. That increases risk and responsibility.

With Hosted UI, passwords are entered only in Google, Microsoft, or Cognito controlled pages. Mejengueros only receives tokens after the provider validates the user.

### PKCE is designed for mobile apps

Mobile apps are public clients. Any secret shipped inside the APK or IPA can be extracted.

PKCE protects the authorization code exchange without requiring a client secret in the app. The app creates a temporary verifier, sends only its challenge during login, and later proves ownership of the original verifier when exchanging the authorization code.

### MFA and account protection stay with the provider

Google and Microsoft already handle:

- multi-factor authentication;
- suspicious login detection;
- account recovery;
- device/account selection;
- provider security policies.

Rebuilding those controls inside Mejengueros would be expensive and riskier.

### Lower phishing risk

Embedded login screens can train users to enter Google or Microsoft credentials into arbitrary app UI.

Hosted UI and system browser flows make it clearer that the user is authenticating with the provider, not typing credentials into Mejengueros.

### Cleaner backend trust boundary

The backend only trusts Cognito-issued tokens. It does not need to accept direct Google or Microsoft tokens from clients.

That gives us one issuer, one token validation strategy, and one place to configure identity providers.

## Why Not Native Google/Microsoft SDKs First

Native SDKs can provide a smoother account picker experience, but they add more moving parts:

- separate SDK setup per platform;
- provider-specific token handling;
- more test cases for each provider;
- extra work to reconcile provider tokens with Cognito/backend identity;
- higher risk of inconsistent behavior between Android, iOS, and Desktop.

For Mejengueros, Hosted UI is the better first implementation because it is simpler, secure, and consistent across platforms.

## User Experience Tradeoff

Hosted UI may open a browser or custom tab instead of keeping every screen inside the app. That is acceptable because the credential step belongs to the identity provider.

To improve the experience without weakening security, the app can store non-sensitive profile hints after login:

- last signed-in email;
- display name;
- provider name.

Then the app can show:

```text
Continue as player@example.com
Use another account
```

Selecting either option still starts the Cognito Hosted UI flow. The app should not display or manage the provider password step itself.

## Token Storage

Tokens and OAuth state are sensitive. They must not be stored in plain SQLite tables.

Current direction:

- Android stores auth material behind encrypted preferences backed by Android security APIs.
- iOS stores auth material through Keychain.
- Desktop/JVM keeps auth material in memory for development only.
- SQLDelight can store non-sensitive cached profile data, but not `idToken`, `accessToken`, `refreshToken`, `state`, or `codeVerifier`.

## Configuration Rules

Public development identifiers may be documented:

- Cognito domain;
- Cognito app client ID;
- redirect URI;
- logout URI;
- API base URL;
- WebSocket URL.

Never commit:

- client secrets;
- real tokens;
- database URLs with passwords;
- provider secrets;
- private keys.

## Final Position

Cognito Hosted UI with Authorization Code Flow and PKCE is the right default for Mejengueros now.

It gives us a secure login foundation, keeps credentials out of the app, centralizes identity in Cognito, and leaves room to improve the user experience later without changing the backend trust model.
