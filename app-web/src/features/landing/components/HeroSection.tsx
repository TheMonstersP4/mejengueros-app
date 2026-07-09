import { ButtonLink } from '../../../shared/components/Button';
import { Icon } from '../../../shared/components/Icon';
import { LandingHeader } from './LandingHeader';
import { StatsStrip } from './StatsStrip';

export function HeroSection() {
  return (
    <section
      id="inicio"
      className="relative overflow-hidden rounded-[26px] border border-white/10 bg-[radial-gradient(78%_62%_at_50%_-6%,rgba(195,244,0,.42),rgba(195,244,0,.08)_30%,#181c12_60%,#0d0f0e_100%)] shadow-panel"
    >
      <div className="field-lines pointer-events-none absolute inset-0 opacity-10" />
      <div className="pointer-events-none absolute left-1/2 top-[56%] h-56 w-56 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-lime/30 md:h-72 md:w-72" />
      <div className="pointer-events-none absolute inset-x-0 top-[56%] h-0.5 bg-lime/25" />
      <LandingHeader />
      <div className="relative px-6 pb-0 pt-14 text-center md:px-14 md:pt-16">
        <span className="inline-flex h-9 items-center gap-2 rounded-full border border-lime/40 bg-black/40 px-4 text-sm font-bold text-lime">
          <span className="h-2 w-2 rounded-full bg-lime animate-live" />
          Canchas reservables hoy
        </span>
        <h1 className="mx-auto mt-7 max-w-5xl font-display text-[clamp(54px,11vw,120px)] uppercase leading-[.86] tracking-[-.02em] text-ink">
          Reservá cancha
          <br />
          <span className="text-lime">y armá la mejenga</span>
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-lg leading-8 text-muted md:text-xl">
          La app tica para encontrar cancha, ver disponibilidad en vivo y reservar tu hora.
          Y si tenés cancha, publicala y llenala.
        </p>
        <div className="mt-9 flex flex-col items-center justify-center gap-4 sm:flex-row">
          <ButtonLink href="#descargar" size="lg">
            <Icon name="download" size={24} className="material-symbols-rounded" />
            Descargar la app
          </ButtonLink>
          <ButtonLink href="#duenos" variant="secondary" size="lg">
            Soy dueño de cancha
          </ButtonLink>
        </div>
        <StatsStrip />
      </div>
    </section>
  );
}
