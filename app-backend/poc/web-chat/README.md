# Web Chat

Static client for testing Cognito, WebSocket, users, private images, and Swagger.

## Flow

- Login with Google or Microsoft through Cognito Hosted UI.
- OAuth callback at `/auth/callback`.
- Protected screen at `/chat/`.
- Internal navigation: `Chat`, `Usuarios`, `Imagenes`, and `Swagger`.
- `Usuarios` calls `/v1/users/me` and `/v1/users`.
- `Imagenes` calls `/v1/files/uploads` and `/v1/files/uploads/confirm`.

## Runtime configuration

Deployment generates `runtime-config.js` from the `DEPLOY_DEV_CONFIG` secret.

```js
window.MEJENGUEROS_CONFIG = {
  cognitoDomain: 'https://...',
  clientId: '...',
  apiBaseUrl: 'https://...',
  websocketUrl: 'wss://...'
};
```

If `apiBaseUrl` is empty, the screen lets you store the API manually in `localStorage`.

## Run locally

```powershell
python -m http.server 3000
```

Open `http://localhost:3000`.
