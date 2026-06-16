const runtimeConfig = window.MEJENGUEROS_CONFIG ?? {};

const config = {
  cognitoDomain: runtimeConfig.cognitoDomain ?? 'https://mejengueros-dev-auth.auth.us-east-2.amazoncognito.com',
  clientId: runtimeConfig.clientId ?? '392mi2ii9l7usot25ksqj58gu6',
  apiBaseUrl: runtimeConfig.apiBaseUrl ?? '',
  redirectUri: `${location.origin}/auth/callback`,
  logoutUri: `${location.origin}/login`,
  protectedPath: '/chat/',
  roomId: 'dev',
  websocketUrl: runtimeConfig.websocketUrl ?? 'wss://dilk66l4f1.execute-api.us-east-2.amazonaws.com/dev',
  providers: {
    google: 'Google',
    microsoft: 'Microsoft'
  }
};

const storageKeys = {
  verifier: 'mejengueros.pkce.verifier',
  state: 'mejengueros.pkce.state',
  tokens: 'mejengueros.tokens',
  apiBaseUrl: 'mejengueros.apiBaseUrl'
};

let socket;
let currentUser;
let connectedUsersCache = [];

const elements = {
  loginGoogle: document.querySelector('#loginGoogle'),
  loginMicrosoft: document.querySelector('#loginMicrosoft'),
  authError: document.querySelector('#authError'),
  logoutButtons: document.querySelectorAll('.logoutButton'),
  navButtons: document.querySelectorAll('.navButton'),
  appViews: document.querySelectorAll('.appView'),
  sessionSummary: document.querySelector('#sessionSummary'),
  currentUserEmail: document.querySelector('#currentUserEmail'),
  sessionBox: document.querySelector('#sessionBox'),
  profileBox: document.querySelector('#profileBox'),
  apiConfigForm: document.querySelector('#apiConfigForm'),
  apiBaseUrlInput: document.querySelector('#apiBaseUrlInput'),
  apiStatus: document.querySelector('#apiStatus'),
  swaggerLink: document.querySelector('#swaggerLink'),
  refreshUsersButton: document.querySelector('#refreshUsersButton'),
  usersList: document.querySelector('#usersList'),
  uploadForm: document.querySelector('#uploadForm'),
  imageInput: document.querySelector('#imageInput'),
  uploadButton: document.querySelector('#uploadButton'),
  uploadStatus: document.querySelector('#uploadStatus'),
  uploadedImages: document.querySelector('#uploadedImages'),
  refreshImagesButton: document.querySelector('#refreshImagesButton'),
  roomStatus: document.querySelector('#roomStatus'),
  wsStatus: document.querySelector('#wsStatus'),
  connectedUsers: document.querySelector('#connectedUsers'),
  messages: document.querySelector('#messages'),
  chatForm: document.querySelector('#chatForm'),
  messageInput: document.querySelector('#messageInput')
};

elements.loginGoogle?.addEventListener('click', () => login(config.providers.google));
elements.loginMicrosoft?.addEventListener('click', () => login(config.providers.microsoft));
elements.logoutButtons.forEach((button) => button.addEventListener('click', logout));
elements.navButtons.forEach((button) => {
  button.addEventListener('click', () => setActiveView(button.dataset.view ?? 'chat'));
});
elements.chatForm?.addEventListener('submit', sendMessage);
elements.apiConfigForm?.addEventListener('submit', saveApiBaseUrl);
elements.uploadForm?.addEventListener('submit', uploadImage);
elements.refreshUsersButton?.addEventListener('click', () => void loadUsers());
elements.refreshImagesButton?.addEventListener('click', () => void loadImages());
window.addEventListener('hashchange', () => setActiveView(readViewFromHash()));

bootstrap();

async function bootstrap() {
  renderAuthError();

  if (await completeLoginIfNeeded()) {
    return;
  }

  const tokens = getTokens();
  const hasSession = hasValidSession(tokens);

  if (isProtectedRoute() && !hasSession) {
    clearSession();
    location.replace('/');
    return;
  }

  if (isAuthCallbackRoute()) {
    setAuthError('No se recibio codigo de autorizacion. Intenta iniciar sesion otra vez.');
    location.replace('/');
    return;
  }

  if (!isProtectedRoute()) {
    if (hasSession) {
      location.replace(config.protectedPath);
    }

    return;
  }

  currentUser = getIdentity(tokens.id_token);
  renderSession(currentUser);
  renderApiConfig();
  setActiveView(readViewFromHash());
  connectWebSocket(tokens.id_token);
  await loadWorkspaceData();
}

async function login(provider) {
  const verifier = base64Url(crypto.getRandomValues(new Uint8Array(32)));
  const state = base64Url(crypto.getRandomValues(new Uint8Array(16)));
  const challenge = await createCodeChallenge(verifier);

  sessionStorage.setItem(storageKeys.verifier, verifier);
  sessionStorage.setItem(storageKeys.state, state);

  const params = new URLSearchParams({
    client_id: config.clientId,
    response_type: 'code',
    scope: 'openid email profile',
    redirect_uri: config.redirectUri,
    code_challenge_method: 'S256',
    code_challenge: challenge,
    state,
    identity_provider: provider
  });

  location.href = `${config.cognitoDomain}/oauth2/authorize?${params.toString()}`;
}

async function completeLoginIfNeeded() {
  const params = new URLSearchParams(location.search);
  const code = params.get('code');
  const state = params.get('state');
  const error = params.get('error');
  const errorDescription = params.get('error_description');

  if (error) {
    setAuthError(errorDescription ?? error);
    location.replace('/');
    return true;
  }

  if (!code) {
    return false;
  }

  const expectedState = sessionStorage.getItem(storageKeys.state);
  const verifier = sessionStorage.getItem(storageKeys.verifier);

  if (!verifier || state !== expectedState) {
    clearSession();
    setAuthError('No se pudo validar la sesion de login. Intenta entrar otra vez.');
    location.replace('/');
    return true;
  }

  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: config.clientId,
    code,
    redirect_uri: config.redirectUri,
    code_verifier: verifier
  });

  try {
    const response = await fetch(`${config.cognitoDomain}/oauth2/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body
    });

    if (!response.ok) {
      const detail = await response.text();
      clearSession();
      setAuthError(`No se pudo completar el login con Cognito. ${detail}`);
      location.replace('/');
      return true;
    }

    const tokens = await response.json();
    localStorage.setItem(storageKeys.tokens, JSON.stringify(tokens));
  } catch (error) {
    clearSession();
    setAuthError(`No se pudo contactar Cognito para completar el login. ${getErrorMessage(error)}`);
    location.replace('/');
    return true;
  }

  sessionStorage.removeItem(storageKeys.verifier);
  sessionStorage.removeItem(storageKeys.state);
  location.replace(config.protectedPath);
  return true;
}

async function loadWorkspaceData() {
  await Promise.allSettled([syncProfile(), loadUsers(), loadImages()]);
}

function connectWebSocket(idToken) {
  if (socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) {
    return;
  }

  setConnectionStatus('Conectando...', 'Preparando sala...');
  socket = new WebSocket(`${config.websocketUrl}?token=${encodeURIComponent(idToken)}`);

  socket.addEventListener('open', () => {
    setConnectionStatus('Conectado', 'Sala activa');
    socket.send(JSON.stringify({
      action: 'joinRoom',
      roomId: config.roomId
    }));
  });

  socket.addEventListener('message', (event) => {
    handleSocketMessage(event.data);
  });

  socket.addEventListener('close', () => {
    setConnectionStatus('Reconectando...', 'Intentando recuperar la sala...');
    window.setTimeout(() => connectWebSocket(idToken), 2000);
  });

  socket.addEventListener('error', () => {
    setConnectionStatus('Sin conexion', 'No se pudo abrir la sala');
  });
}

function sendMessage(event) {
  event.preventDefault();

  const value = elements.messageInput?.value.trim();

  if (!value || socket?.readyState !== WebSocket.OPEN) {
    return;
  }

  socket.send(JSON.stringify({
    action: 'sendMessage',
    roomId: config.roomId,
    message: value,
    sentAt: new Date().toISOString()
  }));
  elements.messageInput.value = '';
}

function handleSocketMessage(rawMessage) {
  const payload = parseSocketPayload(rawMessage);

  if (payload.type === 'presence') {
    connectedUsersCache = payload.users ?? [];
    renderConnectedUsers(connectedUsersCache);
    return;
  }

  if (payload.type === 'message') {
    addMessage({
      mine: payload.sender?.sub === currentUser?.sub,
      name: payload.sender?.name ?? payload.sender?.email ?? 'Usuario',
      text: payload.message
    });
  }
}

function parseSocketPayload(rawMessage) {
  try {
    return JSON.parse(rawMessage);
  } catch {
    return {
      type: 'message',
      message: rawMessage
    };
  }
}

function logout() {
  clearSession();
  socket?.close();

  const params = new URLSearchParams({
    client_id: config.clientId,
    logout_uri: config.logoutUri
  });

  location.href = `${config.cognitoDomain}/logout?${params.toString()}`;
}

function renderAuthError() {
  if (!elements.authError) {
    return;
  }

  const message = sessionStorage.getItem('mejengueros.auth.error');

  if (!message) {
    return;
  }

  sessionStorage.removeItem('mejengueros.auth.error');
  elements.authError.textContent = message;
  elements.authError.classList.remove('hidden');
}

function setAuthError(message) {
  sessionStorage.setItem('mejengueros.auth.error', message);
}

function renderSession(identity) {
  if (elements.currentUserEmail) {
    elements.currentUserEmail.textContent = identity.email;
  }

  if (elements.sessionSummary) {
    elements.sessionSummary.textContent = `Conectado como ${identity.email}`;
  }

  if (!elements.sessionBox) {
    return;
  }

  elements.sessionBox.innerHTML = `
    ${identity.pictureUrl ? `<img class="mb-3 h-16 w-16 rounded-2xl object-cover shadow-sm" src="${escapeHtml(identity.pictureUrl)}" alt="Foto de perfil" />` : ''}
    <div class="font-bold text-ink">${escapeHtml(identity.name)}</div>
    <div>${escapeHtml(identity.email)}</div>
    <div class="mt-2 break-all text-xs text-ink/45">Sub: ${escapeHtml(identity.sub)}</div>
    <div class="text-xs text-ink/45">Provider: ${escapeHtml(identity.provider ?? 'n/a')}</div>
    <div class="break-all text-xs text-ink/45">Issuer: ${escapeHtml(identity.issuer ?? 'n/a')}</div>
  `;
}

async function syncProfile() {
  if (!elements.profileBox) {
    return;
  }

  if (!getApiBaseUrl()) {
    elements.profileBox.innerHTML = emptyState('Configura la API para sincronizar el perfil.');
    return;
  }

  elements.profileBox.innerHTML = 'Sincronizando usuario...';

  try {
    const profile = await apiRequest('/users/me');

    elements.profileBox.innerHTML = userCard(profile);
  } catch (error) {
    elements.profileBox.innerHTML = errorState('No se pudo leer /users/me', error);
  }
}

async function loadUsers() {
  if (!elements.usersList) {
    return;
  }

  if (!getApiBaseUrl()) {
    elements.usersList.innerHTML = emptyState('Configura la API para listar usuarios.');
    return;
  }

  elements.usersList.innerHTML = emptyState('Cargando usuarios...');

  try {
    const users = await apiRequest('/users');
    renderUsersList(Array.isArray(users) ? users : []);
  } catch (error) {
    elements.usersList.innerHTML = errorState('No se pudo leer /users', error);
  }
}

function renderUsersList(users) {
  if (!elements.usersList) {
    return;
  }

  if (users.length === 0) {
    elements.usersList.innerHTML = emptyState('Aun no hay usuarios sincronizados.');
    return;
  }

  elements.usersList.innerHTML = users.map(userCard).join('');
}

async function loadImages() {
  if (!elements.uploadedImages) {
    return;
  }

  if (!getApiBaseUrl()) {
    elements.uploadedImages.innerHTML = emptyState('Configura la API para listar imagenes.');
    return;
  }

  elements.uploadedImages.innerHTML = emptyState('Cargando imagenes...');

  try {
    const images = await apiRequest('/files/uploads');
    renderImageUploads(Array.isArray(images) ? images : []);
  } catch (error) {
    elements.uploadedImages.innerHTML = errorState('No se pudo leer /files/uploads', error);
  }
}

function renderImageUploads(images) {
  if (!elements.uploadedImages) {
    return;
  }

  if (images.length === 0) {
    elements.uploadedImages.innerHTML = emptyState('Aun no hay imagenes.');
    return;
  }

  elements.uploadedImages.innerHTML = images
    .map((image) => `
      <article class="overflow-hidden rounded-3xl bg-white/80 shadow-sm">
        <img class="h-48 w-full object-cover" src="${escapeHtml(image.readUrl)}" alt="Imagen subida" />
        <div class="space-y-2 p-4 text-sm text-ink/65">
          <div class="font-black text-ink">${escapeHtml(image.uploadedBy?.name ?? image.uploadedBy?.email ?? 'Usuario')}</div>
          <div>${escapeHtml(image.uploadedBy?.email ?? 'Sin email')}</div>
          <div>${escapeHtml(image.contentType)} - ${formatBytes(image.sizeBytes)}</div>
          <div class="break-all text-xs text-ink/45">${escapeHtml(image.objectKey)}</div>
          <div class="text-xs text-ink/45">${formatDate(image.createdAt)}</div>
        </div>
      </article>
    `)
    .join('');
}

function renderApiConfig() {
  const apiBaseUrl = getApiBaseUrl();

  if (elements.apiBaseUrlInput) {
    elements.apiBaseUrlInput.value = apiBaseUrl;
  }

  if (elements.swaggerLink) {
    elements.swaggerLink.href = apiBaseUrl ? `${apiBaseUrl}/docs` : '#';
    elements.swaggerLink.classList.toggle('opacity-50', !apiBaseUrl);
  }
}

function saveApiBaseUrl(event) {
  event.preventDefault();

  const value = normalizeApiBaseUrl(elements.apiBaseUrlInput?.value ?? '');

  if (value) {
    localStorage.setItem(storageKeys.apiBaseUrl, value);
  } else {
    localStorage.removeItem(storageKeys.apiBaseUrl);
  }

  renderApiConfig();
  setApiStatus(value ? 'API guardada.' : 'API URL limpia.');
  void loadWorkspaceData();
}

async function uploadImage(event) {
  event.preventDefault();

  const file = elements.imageInput?.files?.[0];

  if (!hasValidSession(getTokens())) {
    setUploadStatus('La sesion expiro. Inicia sesion otra vez.');
    return;
  }

  if (!getApiBaseUrl()) {
    setUploadStatus('Configura la API antes de subir imagenes.');
    return;
  }

  if (!file) {
    setUploadStatus('Selecciona una imagen primero.');
    return;
  }

  setUploadBusy(true);

  try {
    setUploadStatus('Preparando carga...');
    const upload = await apiRequest('/files/uploads', {
      method: 'POST',
      body: {
        purpose: 'profile-image',
        contentType: file.type,
        sizeBytes: file.size
      }
    });

    setUploadStatus('Subiendo a S3...');
    await uploadToStorage(upload, file);

    setUploadStatus('Confirmando imagen...');
    await apiRequest('/files/uploads/confirm', {
      method: 'POST',
      body: {
        purpose: 'profile-image',
        objectKey: upload.objectKey
      }
    });

    elements.imageInput.value = '';
    setUploadStatus('Imagen lista.');
    await loadImages();
  } catch (error) {
    setUploadStatus(getErrorMessage(error));
  } finally {
    setUploadBusy(false);
  }
}

async function uploadToStorage(upload, file) {
  const form = new FormData();

  Object.entries(upload.fields).forEach(([key, value]) => {
    form.append(key, value);
  });

  form.set('Content-Type', file.type);
  form.append('file', file);

  const response = await fetch(upload.uploadUrl, {
    method: upload.method,
    body: form
  });

  if (!response.ok) {
    throw new Error(`S3 rechazo la carga con status ${response.status}.`);
  }
}

async function apiRequest(path, options = {}) {
  const tokens = getTokens();
  const idToken = options.idToken ?? tokens?.id_token;

  if (!idToken) {
    throw new Error('Sesion no disponible.');
  }

  const response = await fetch(buildApiUrl(path), {
    method: options.method ?? 'GET',
    headers: {
      Authorization: `Bearer ${idToken}`,
      ...(options.body ? { 'Content-Type': 'application/json' } : {})
    },
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  const payload = await response.json().catch(() => null);

  if (!response.ok || payload?.success === false) {
    const message = payload?.errors?.[0]?.message ?? `API respondio con status ${response.status}.`;
    throw new Error(message);
  }

  return payload?.data ?? payload;
}

function buildApiUrl(path) {
  const baseUrl = getApiBaseUrl();

  if (!baseUrl) {
    throw new Error('API URL no configurada.');
  }

  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const versionedBaseUrl = baseUrl.endsWith('/v1') ? baseUrl : `${baseUrl}/v1`;

  return `${versionedBaseUrl}${normalizedPath}`;
}

function getApiBaseUrl() {
  return localStorage.getItem(storageKeys.apiBaseUrl) ?? config.apiBaseUrl;
}

function normalizeApiBaseUrl(value) {
  return value.trim().replace(/\/+$/, '').replace(/\/v1$/, '');
}

function setApiStatus(message) {
  if (elements.apiStatus) {
    elements.apiStatus.textContent = message;
  }
}

function setUploadStatus(message) {
  if (elements.uploadStatus) {
    elements.uploadStatus.textContent = message;
  }
}

function setUploadBusy(isBusy) {
  if (elements.uploadButton) {
    elements.uploadButton.disabled = isBusy;
    elements.uploadButton.textContent = isBusy ? 'Subiendo...' : 'Subir imagen';
    elements.uploadButton.classList.toggle('opacity-60', isBusy);
  }
}

function renderConnectedUsers(users) {
  if (!elements.connectedUsers) {
    return;
  }

  if (users.length === 0) {
    elements.connectedUsers.innerHTML = emptyState('Sin usuarios conectados.');
    return;
  }

  elements.connectedUsers.innerHTML = users
    .map((user) => `
      <div class="rounded-2xl bg-white/75 px-4 py-3">
        <div class="font-bold text-ink">${escapeHtml(user.name ?? user.email ?? 'Usuario')}</div>
        <div class="text-xs text-ink/50">${escapeHtml(user.email ?? '')}</div>
      </div>
    `)
    .join('');
}

function setConnectionStatus(wsStatus, roomStatus) {
  if (elements.wsStatus) {
    elements.wsStatus.textContent = wsStatus;
  }

  if (elements.roomStatus) {
    elements.roomStatus.textContent = roomStatus;
  }
}

function setActiveView(view) {
  const nextView = ['chat', 'users', 'images'].includes(view) ? view : 'chat';

  elements.appViews.forEach((panel) => {
    panel.classList.toggle('hidden', panel.dataset.viewPanel !== nextView);
  });
  elements.navButtons.forEach((button) => {
    const isActive = button.dataset.view === nextView;
    button.classList.toggle('bg-ink', isActive);
    button.classList.toggle('text-white', isActive);
    button.classList.toggle('bg-white', !isActive);
    button.classList.toggle('text-ink', !isActive);
  });

  if (location.hash !== `#${nextView}`) {
    history.replaceState(null, '', `#${nextView}`);
  }
}

function readViewFromHash() {
  return location.hash.replace('#', '') || 'chat';
}

function userCard(user) {
  return `
    <article class="rounded-3xl bg-white/75 p-4 text-sm text-ink/70 shadow-sm">
      ${user.pictureUrl ? `<img class="mb-3 h-14 w-14 rounded-2xl object-cover" src="${escapeHtml(user.pictureUrl)}" alt="Foto de perfil" />` : ''}
      <div class="font-black text-ink">${escapeHtml(user.name ?? 'Usuario')}</div>
      <div>${escapeHtml(user.email ?? 'Sin email')}</div>
      <div class="mt-2 break-all text-xs text-ink/45">ID: ${escapeHtml(user.id ?? 'n/a')}</div>
      <div class="break-all text-xs text-ink/45">Sub: ${escapeHtml(user.cognitoSub ?? user.sub ?? 'n/a')}</div>
      <div class="text-xs text-ink/45">Provider: ${escapeHtml(user.provider ?? 'n/a')}</div>
    </article>
  `;
}

function emptyState(message) {
  return `<div class="rounded-3xl bg-white/75 px-4 py-3 text-sm text-ink/60">${escapeHtml(message)}</div>`;
}

function errorState(title, error) {
  return `
    <div class="rounded-3xl bg-white/75 px-4 py-3 text-sm text-clay">
      <div class="font-black">${escapeHtml(title)}</div>
      <div>${escapeHtml(getErrorMessage(error))}</div>
    </div>
  `;
}

function isProtectedRoute() {
  return location.pathname.replace(/\/?$/, '/') === config.protectedPath;
}

function isAuthCallbackRoute() {
  return location.pathname.replace(/\/?$/, '/') === '/auth/callback/';
}

function clearSession() {
  localStorage.removeItem(storageKeys.tokens);
}

function getErrorMessage(error) {
  return error instanceof Error ? error.message : String(error);
}

function getTokens() {
  const raw = localStorage.getItem(storageKeys.tokens);
  return raw ? JSON.parse(raw) : null;
}

function hasValidSession(tokens) {
  if (!tokens?.id_token) {
    return false;
  }

  try {
    const identity = decodeJwt(tokens.id_token);
    const expiresAt = Number(identity.exp ?? 0) * 1000;

    return expiresAt > Date.now();
  } catch {
    return false;
  }
}

function getIdentity(token) {
  const payload = decodeJwt(token);

  return {
    sub: payload.sub,
    name: payload.name ?? payload.email ?? payload['cognito:username'] ?? 'Usuario',
    email: payload.email ?? 'Sin email',
    pictureUrl: payload.picture,
    provider: getTokenProvider(payload),
    issuer: payload.iss
  };
}

function getTokenProvider(payload) {
  const identities = parseIdentitiesClaim(payload.identities);

  return identities[0]?.providerName ?? payload['cognito:username']?.split('_')[0];
}

function parseIdentitiesClaim(value) {
  if (Array.isArray(value)) {
    return value;
  }

  if (typeof value !== 'string') {
    return [];
  }

  try {
    return JSON.parse(value);
  } catch {
    return [];
  }
}

function decodeJwt(token) {
  const [, payload] = token.split('.');
  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
  return JSON.parse(atob(padded));
}

async function createCodeChallenge(verifier) {
  const bytes = new TextEncoder().encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', bytes);
  return base64Url(new Uint8Array(digest));
}

function base64Url(bytes) {
  const value = btoa(String.fromCharCode(...bytes));
  return value.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function addMessage(input) {
  if (!elements.messages) {
    return;
  }

  const message = document.createElement('div');
  const align = input.mine ? 'ml-auto bg-ink text-white' : 'mr-auto bg-white text-ink';

  message.className = `max-w-[82%] rounded-2xl px-4 py-3 text-sm shadow-sm ${align}`;
  message.innerHTML = `
    <div class="mb-1 text-xs font-bold opacity-60">${escapeHtml(input.name)}</div>
    <div>${escapeHtml(input.text)}</div>
  `;
  elements.messages.appendChild(message);
  elements.messages.scrollTop = elements.messages.scrollHeight;
}

function formatBytes(value) {
  const size = Number(value);

  if (!Number.isFinite(size)) {
    return '0 B';
  }

  if (size < 1024) {
    return `${size} B`;
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }

  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return '';
  }

  return date.toLocaleString('es-CR', {
    dateStyle: 'medium',
    timeStyle: 'short'
  });
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
