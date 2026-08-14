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

export const productFeatureHighlights = [
  {
    label: 'Acceso',
    title: 'Entrar rápido',
    body: 'Registro con correo y login social para empezar sin vueltas.',
  },
  {
    label: 'Búsqueda',
    title: 'Encontrar cancha',
    body: 'Filtros por zona, horario y disponibilidad para elegir dónde jugar.',
  },
  {
    label: 'Reservas',
    title: 'Apartar horario',
    body: 'Reservas claras para jugadores y menos coordinación manual para dueños.',
  },
  {
    label: 'Confianza',
    title: 'Reseñas reales',
    body: 'Calificaciones, comentarios y fotos para conocer mejor cada cancha.',
  },
  {
    label: 'Avisos',
    title: 'Notificaciones',
    body: 'Recordatorios y avisos para dar seguimiento antes y después de jugar.',
  },
] as const;
