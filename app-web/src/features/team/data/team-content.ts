export const teamMembers = [
  {
    name: 'Shanty Cerdas',
    initials: 'SC',
  },
  {
    name: 'David Gutiérrez',
    initials: 'DG',
  },
  {
    name: 'Maxwell Chinchilla',
    initials: 'MC',
  },
  {
    name: 'Daniel Nazario',
    initials: 'DN',
  },
  {
    name: 'Carl Levey',
    initials: 'CL',
  },
] as const;

export const teamPrinciples = [
  'Cambios pequeños y con alcance claro.',
  'Contratos backend probados antes de que la UI dependa de ellos.',
  'Flujos móviles validados con recorridos reales de usuario.',
  'Cambios de infraestructura documentados antes del despliegue.',
] as const;

export const deliveryTracks = [
  {
    label: 'Producto',
    value: 'Backlog claro',
    body: 'Issues con criterios de aceptación, dependencias y una definición de hecho revisable.',
  },
  {
    label: 'Aplicación',
    value: 'KMP + React',
    body: 'Aplicación móvil para la experiencia principal y web pública para explicar el producto.',
  },
  {
    label: 'Infraestructura',
    value: 'Serverless en AWS',
    body: 'API, WebSocket, almacenamiento y tareas programadas con despliegue automatizado.',
  },
] as const;

export const cloudArchitectureStages = [
  {
    label: 'Entrada',
    title: 'Dominio + hosting',
    body: 'Sitio público, rutas de desarrollo y entrada al ecosistema cloud.',
  },
  {
    label: 'Autenticación',
    title: 'Cognito',
    body: 'Login con correo, Google y Microsoft.',
  },
  {
    label: 'Backend',
    title: 'API Gateway + Lambda',
    body: 'Contratos HTTP para usuarios, reservas, imágenes y notificaciones.',
  },
  {
    label: 'Datos',
    title: 'S3 + PostgreSQL',
    body: 'Imágenes en S3 y datos relacionales en PostgreSQL.',
  },
  {
    label: 'Tiempo real',
    title: 'WebSocket + EventBridge',
    body: 'Avisos en vivo y tareas programadas para cerrar reservas.',
  },
] as const;
