import { ButtonLink } from '../../shared/components/Button';
import { Icon } from '../../shared/components/Icon';
import {
  cloudArchitectureStages,
  deliveryTracks,
  teamMembers,
  teamPrinciples,
} from '../../features/team/data/team-content';

function TeamHeader() {
  return (
    <header className="relative z-10 flex flex-wrap items-center gap-5 px-6 py-6 md:px-14">
      <a
        href="/"
        className="font-display text-2xl italic uppercase tracking-[-.01em] text-ink no-underline"
      >
        mejengueros
      </a>
      <nav
        aria-label="Navegación pública"
        className="ml-auto flex flex-wrap items-center gap-5 text-[15px] font-medium text-muted"
      >
        <a href="/" className="transition hover:text-lime">
          Producto
        </a>
        <a href="/team" aria-current="page" className="text-lime">
          Equipo
        </a>
      </nav>
    </header>
  );
}

function TeamHero() {
  return (
    <section className="relative overflow-hidden rounded-[26px] border border-white/10 bg-[radial-gradient(86%_72%_at_50%_-12%,rgba(195,244,0,.34),rgba(195,244,0,.06)_36%,#181c12_62%,#0d0f0e_100%)] shadow-panel">
      <div className="field-lines pointer-events-none absolute inset-0 opacity-10" />
      <TeamHeader />
      <div className="relative grid gap-10 px-6 pb-16 pt-12 md:grid-cols-[1.1fr_.9fr] md:px-14 md:pb-24 md:pt-16">
        <div>
          <span className="inline-flex h-9 items-center rounded-full border border-lime/40 bg-black/40 px-4 text-sm font-bold text-lime">
            The Monsters
          </span>
          <h1 className="mt-7 max-w-4xl font-display text-[clamp(52px,9vw,108px)] uppercase leading-[.86] tracking-[-.02em] text-ink">
            El equipo
            <br />
            <span className="text-lime">detrás de la mejenga</span>
          </h1>
          <p className="mt-6 max-w-2xl text-lg leading-8 text-muted md:text-xl">
            Somos el grupo que diseña, construye y valida Mejengueros: una app
            para conectar jugadores con canchas disponibles y ayudar a los
            dueños a llenar horarios.
          </p>
          <div className="mt-9 flex flex-col gap-4 sm:flex-row">
            <ButtonLink href="#integrantes" size="lg">
              Ver integrantes
            </ButtonLink>
            <ButtonLink href="/" variant="secondary" size="lg">
              Volver al producto
            </ButtonLink>
          </div>
        </div>
        <div className="rounded-[24px] border border-lime/25 bg-black/35 p-6 backdrop-blur-xl">
          <p className="text-[13px] font-bold uppercase tracking-[.18em] text-lime">
            Cómo trabajamos
          </p>
          <ul className="mt-6 grid gap-4">
            {teamPrinciples.map((principle) => (
              <li key={principle} className="flex gap-3 rounded-2xl bg-surface/80 p-4">
                <Icon name="check_circle" size={24} className="text-lime" />
                <span className="text-base leading-6 text-ink">{principle}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

function DeliveryTrackSection() {
  return (
    <section className="grid gap-4 px-6 py-14 md:grid-cols-3 md:px-14">
      {deliveryTracks.map((track) => (
        <article key={track.label} className="rounded-[20px] border border-white/10 bg-surfaceAlt p-6">
          <p className="text-[12px] font-bold uppercase tracking-[.16em] text-faint">
            {track.label}
          </p>
          <h2 className="mt-3 font-display text-3xl uppercase text-ink">{track.value}</h2>
          <p className="mt-3 text-base leading-7 text-muted">{track.body}</p>
        </article>
      ))}
    </section>
  );
}

function CloudArchitectureSection() {
  return (
    <div className="mb-10">
      <div className="rounded-[28px] border border-lime/20 bg-[linear-gradient(135deg,rgba(195,244,0,.12),rgba(24,28,18,.7)_42%,rgba(10,12,12,.96))] p-6 shadow-panel md:p-8">
        <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-[13px] font-bold uppercase tracking-[.18em] text-lime">
              Infraestructura
            </p>
            <h2 className="mt-3 font-display text-5xl uppercase text-ink">
              Arquitectura cloud
            </h2>
          </div>
          <p className="max-w-xl text-base leading-7 text-muted">
            El flujo actual combina autenticación administrada, funciones serverless,
            almacenamiento y comunicación en tiempo real.
          </p>
        </div>
        <div className="mt-8 grid gap-4 lg:grid-cols-5">
          {cloudArchitectureStages.map((stage, index) => (
            <article
              key={stage.label}
              className="relative rounded-[22px] border border-white/10 bg-black/35 p-5"
            >
              {index < cloudArchitectureStages.length - 1 ? (
                <span
                  aria-hidden="true"
                  className="absolute -right-4 top-1/2 hidden h-px w-4 bg-lime/50 lg:block"
                />
              ) : null}
              <p className="text-[11px] font-bold uppercase tracking-[.16em] text-lime">
                {stage.label}
              </p>
              <h3 className="mt-4 font-display text-2xl uppercase text-ink">
                {stage.title}
              </h3>
              <p className="mt-3 text-sm leading-6 text-muted">{stage.body}</p>
            </article>
          ))}
        </div>
      </div>
    </div>
  );
}

function TeamMembersSection() {
  return (
    <section id="integrantes" className="px-6 pb-16 md:px-14 md:pb-[72px]">
      <div className="mb-8">
        <div>
          <p className="text-[13px] font-bold uppercase tracking-[.18em] text-lime">
            Integrantes
          </p>
          <h2 className="mt-3 font-display text-5xl uppercase text-ink">
            Roles claros, mismo objetivo
          </h2>
        </div>
      </div>
      <CloudArchitectureSection />
      <div className="grid gap-5 md:grid-cols-2">
        {teamMembers.map((member) => (
          <article
            key={member.name}
            className="group rounded-[22px] border border-white/10 bg-[rgba(45,49,77,.42)] p-6 transition hover:border-lime/45"
          >
            <div className="flex items-start gap-5">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl border border-lime/35 bg-lime text-xl font-black text-limeInk shadow-lime">
                {member.initials}
              </div>
              <div>
                <h3 className="font-display text-3xl uppercase text-ink">{member.name}</h3>
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function TeamFooter() {
  return (
    <footer className="flex flex-col gap-5 border-t border-white/10 px-6 py-10 md:flex-row md:items-center md:px-14">
      <a href="/" className="font-display text-xl italic uppercase text-ink no-underline">
        mejengueros
      </a>
      <nav aria-label="Navegación secundaria" className="flex flex-wrap gap-5 text-sm text-muted">
        <a href="/" className="transition hover:text-lime">
          Producto
        </a>
        <a href="/team" className="text-lime">
          Equipo
        </a>
      </nav>
      <span className="text-[13px] text-faint md:ml-auto">
        The Monsters · construyendo desde Costa Rica
      </span>
    </footer>
  );
}

export function TeamPage() {
  return (
    <main className="min-h-screen bg-pitch p-4 text-ink md:p-8 lg:p-14">
      <div className="mx-auto max-w-[1280px] overflow-hidden rounded-[26px] border border-white/10 bg-pitch shadow-panel">
        <TeamHero />
        <DeliveryTrackSection />
        <TeamMembersSection />
        <TeamFooter />
      </div>
    </main>
  );
}
