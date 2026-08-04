import { Icon } from '../../../shared/components/Icon';

type PathCardProps = {
  id?: string;
  icon: string;
  accentClassName: string;
  title: string;
  body: string;
  cta: string;
  href: string;
};

function PathCard({ id, icon, accentClassName, title, body, cta, href }: PathCardProps) {
  return (
    <article
      id={id}
      className="rounded-[20px] border border-white/10 bg-[rgba(45,49,77,.42)] p-8 backdrop-blur-xl md:p-10"
    >
      <Icon name={icon} size={36} className={`material-symbols-rounded ${accentClassName}`} />
      <h2 className="mt-4 font-display text-3xl uppercase text-ink">{title}</h2>
      <p className="mt-2 text-base leading-7 text-muted">{body}</p>
      <a
        href={href}
        className={`mt-5 inline-flex items-center gap-2 font-display text-[15px] uppercase tracking-[.02em] no-underline transition hover:translate-x-1 ${accentClassName}`}
      >
        {cta}
        <Icon name="arrow_forward" className="material-symbols-rounded" />
      </a>
    </article>
  );
}

export function AudiencePathsSection() {
  return (
    <section
      id="audiencias"
      aria-labelledby="audience-paths-title"
      className="grid gap-6 px-6 py-16 md:grid-cols-2 md:px-14 md:py-[72px]"
    >
      <h2 id="audience-paths-title" className="sr-only">
        Caminos principales de la landing
      </h2>
      <PathCard
        id="jugadores"
        icon="sports_soccer"
        accentClassName="text-lime"
        title="Para jugadores"
        body="Buscá canchas cerca, mirá horas libres y reservá al toque. Llevá tus reservas y reseñas siempre con vos."
        cta="Explorar canchas"
        href="#descargar"
      />
      <PathCard
        id="duenos"
        icon="stadium"
        accentClassName="text-owner"
        title="Para dueños"
        body="Publicá tu complejo, configurá disponibilidad y recibí reservas sin coordinar por teléfono. Gratis para empezar."
        cta="Publicar mi cancha"
        href="#duenos"
      />
    </section>
  );
}
