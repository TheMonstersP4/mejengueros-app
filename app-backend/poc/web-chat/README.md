# Web Chat

Cliente estatico para probar Cognito, WebSocket, usuarios, imagenes privadas y Swagger.

## Flujo

- Login con Google o Microsoft mediante Cognito Hosted UI.
- Callback OAuth en `/auth/callback`.
- Pantalla protegida en `/chat/`.
- Navegacion interna: `Chat`, `Usuarios`, `Imagenes` y `Swagger`.
- `Usuarios` consume `/v1/users/me` y `/v1/users`.
- `Imagenes` consume `/v1/files/uploads` y `/v1/files/uploads/confirm`.

## Configuracion runtime

El despliegue genera `runtime-config.js` desde el secret `DEPLOY_DEV_CONFIG`.

```js
window.MEJENGUEROS_CONFIG = {
  cognitoDomain: 'https://...',
  clientId: '...',
  apiBaseUrl: 'https://...',
  websocketUrl: 'wss://...'
};
```

Si `apiBaseUrl` queda vacio, la pantalla permite guardar la API manualmente en `localStorage`.

## Correr local

```powershell
python -m http.server 3000
```

Abrir `http://localhost:3000`.
