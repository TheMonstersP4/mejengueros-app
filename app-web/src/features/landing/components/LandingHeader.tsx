import { ButtonLink } from '../../../shared/components/Button';
import { Icon } from '../../../shared/components/Icon';
import { navigationLinks } from '../data/landing-content';

export function LandingHeader() {
  return (
    <header className="relative z-10 flex items-center gap-7 px-6 py-6 md:px-14">
      <a
        href="#inicio"
        className="font-display text-2xl italic uppercase tracking-[-.01em] text-ink no-underline"
      >
        mejengueros
      </a>
      <nav
        aria-label="Navegación principal"
        className="ml-6 hidden gap-7 text-[15px] font-medium text-muted md:flex"
      >
        {navigationLinks.map((link) => (
          <a key={link.href} href={link.href} className="transition hover:text-lime">
            {link.label}
          </a>
        ))}
      </nav>
      <ButtonLink href="#descargar" className="ml-auto hidden md:inline-flex">
        <Icon name="download" className="material-symbols-rounded" />
        Descargar app
      </ButtonLink>
    </header>
  );
}
