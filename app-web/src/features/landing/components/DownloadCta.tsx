import { StoreBadge } from '../../../shared/components/StoreBadge';
import { storeLinks } from '../data/landing-content';

export function DownloadCta() {
  return (
    <section
      id="descargar"
      className="mx-6 mb-16 flex flex-col gap-8 rounded-3xl bg-lime p-8 md:mx-14 md:mb-[72px] md:flex-row md:items-center md:p-14"
    >
      <div className="flex-1">
        <h2 className="font-display text-5xl uppercase leading-[.95] text-limeInk">
          Descargá
          <br />
          Mejengueros
        </h2>
        <p className="mt-4 max-w-xl text-lg font-semibold text-[#2a3400]">
          Gratis para iOS y Android. Tu próxima mejenga está a un toque.
        </p>
      </div>
      <div className="flex flex-col gap-3 sm:flex-row md:flex-col">
        {storeLinks.map((store) => (
          <StoreBadge
            key={store.href}
            icon={store.icon}
            kicker={store.kicker}
            label={store.label}
            href={store.href}
          />
        ))}
      </div>
    </section>
  );
}
