import { navigationLinks } from '../data/landing-content';

export function LandingFooter() {
  return (
    <footer className="flex flex-col gap-5 border-t border-white/10 px-6 py-10 md:flex-row md:items-center md:px-14">
      <a
        href="#inicio"
        className="font-display text-xl italic uppercase text-ink no-underline"
      >
        mejengueros
      </a>
      <nav
        aria-label="Navegación secundaria"
        className="flex flex-wrap gap-5 text-sm text-muted md:ml-3"
      >
        {navigationLinks.map((link) => (
          <a key={link.href} href={link.href} className="transition hover:text-lime">
            {link.label}
          </a>
        ))}
        <a href="#soporte" className="transition hover:text-lime">
          Soporte
        </a>
        <a href="#privacidad" className="transition hover:text-lime">
          Privacidad
        </a>
      </nav>
      <span className="text-[13px] text-faint md:ml-auto">Hecho en Costa Rica · Pura vida</span>
    </footer>
  );
}
