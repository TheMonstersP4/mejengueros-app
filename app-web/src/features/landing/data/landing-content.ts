export const navigationLinks = [
  { label: 'Jugadores', href: '#jugadores' },
  { label: 'Dueños', href: '#duenos' },
  { label: 'Cómo funciona', href: '#como-funciona' },
] as const;

export const landingStats = [
  { value: '120+', label: 'Canchas', accent: true },
  { value: '4.7', label: 'Rating promedio', accent: false },
  { value: '1 min', label: 'Para reservar', accent: false },
] as const;

export const landingSteps = [
  {
    number: '01',
    title: 'Buscá',
    body: 'Filtrá por zona y tipo de fútbol. Todo lo publicado y activo.',
  },
  {
    number: '02',
    title: 'Elegí la hora',
    body: 'Disponibilidad en vivo en slots de 1 hora.',
  },
  {
    number: '03',
    title: 'Reservá y jugá',
    body: 'Confirmá, jugá tu mejenga y dejá tu reseña.',
  },
] as const;

export const playerBenefits = [
  { icon: 'bolt', text: 'Disponibilidad en vivo, sin llamar' },
  { icon: 'touch_app', text: 'Reservá en segundos' },
  { icon: 'reviews', text: 'Reseñas reales de la comunidad' },
] as const;

export const ownerBenefits = [
  { icon: 'storefront', text: 'Publicá tu complejo gratis' },
  { icon: 'calendar_month', text: 'Gestioná horarios por cancha' },
  { icon: 'trending_up', text: 'Más reservas, menos coordinación' },
] as const;

export const storeLinks = [
  {
    label: 'App Store',
    kicker: 'Descargá en',
    icon: 'apple',
    href: '#app-store',
  },
  {
    label: 'Google Play',
    kicker: 'Disponible en',
    icon: 'shop',
    href: '#google-play',
  },
] as const;
